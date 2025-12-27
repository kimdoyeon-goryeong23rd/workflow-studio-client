package com.saltlux.workflow.direct;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.core.codec.CodecException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saltlux.workflow.core.common.WorkflowContext;
import com.saltlux.workflow.core.common.WorkflowListener;
import com.saltlux.workflow.direct.common.ChatMessageAssembler;
import com.saltlux.workflow.direct.common.CitedMessageAssembler;
import com.saltlux.workflow.direct.common.DirectLlmClientException;
import com.saltlux.workflow.direct.payload.DirectLlmRequest;
import com.saltlux.workflow.direct.payload.chatcompletion.AdvancedCompletionResponse;
import com.saltlux.workflow.direct.payload.chatcompletion.BaseCompletionResponse;
import com.saltlux.workflow.direct.payload.chatcompletion.ChatCompletionResponse;
import com.saltlux.workflow.direct.payload.messageable.CitedMessage;
import com.saltlux.workflow.direct.payload.messageable.ResponseMessage;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * OpenAI 호환 Chat Completion API 스트리밍 클라이언트.
 *
 * <p>
 * 워크플로우를 거치지 않고 직접 LLM API를 호출하여 SSE 스트리밍 응답을 받습니다.
 * Spring 의존성 없이 직접 생성하여 사용한다.
 * </p>
 */
@Slf4j
public class DirectLlmProcessor {

  /** Chat Completion Stream Response DONE 검출 로직 */
  private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

  private final WebClient client;
  private final ObjectMapper objectMapper;

  public DirectLlmProcessor(
      final WebClient.Builder clientBuilder,
      final ObjectMapper objectMapper) {
    this.client = clientBuilder.build();
    this.objectMapper = objectMapper;
  }

  private ChatCompletionResponse parseChatResponse(final String json) {
    try {
      return objectMapper.readValue(json, ChatCompletionResponse.class);
    } catch (JsonProcessingException e) {
      throw new DirectLlmClientException(
          "Failed to parse SSE data: " + json.substring(0, Math.min(200, json.length())),
          e);
    }
  }

  /**
   * Chat Completion 스트리밍 raw 요청.
   *
   * <p>
   * 인용 처리 없이 원본 {@link ChatCompletionResponse}를 그대로 반환한다.
   * 내부 모듈(DeepresearchProcessor 등)에서만 사용하며, WorkflowClient에 노출하지 않는다.
   * </p>
   *
   * @param directLlmRequest LLM 직접 호출 요청 정보
   * @return 원본 스트리밍 응답 Flux
   */
  public Flux<ChatCompletionResponse> streamRaw(final DirectLlmRequest directLlmRequest) {
    return this.client.post()
        .uri(directLlmRequest.getBaseUrl() + "/v1/chat/completions")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .headers(headers -> {
          headers.setContentType(MediaType.APPLICATION_JSON);
          if (directLlmRequest.getApiKey() != null &&
              !directLlmRequest.getApiKey().isBlank()) {
            headers.setBearerAuth(directLlmRequest.getApiKey());
          }
        })
        .bodyValue(directLlmRequest.getBody().toChatCompletionRequest())
        .retrieve()
        .bodyToFlux(String.class)
        .doOnNext(raw -> log.info("[RAW] {}", raw))
        .takeUntil(SSE_DONE_PREDICATE)
        .filter(SSE_DONE_PREDICATE.negate())
        .map(this::parseChatResponse)
        .onErrorMap(e -> switch (e) {
          case DirectLlmClientException ex -> ex;
          case CodecException ex -> new DirectLlmClientException("Failed to serialize/deserialize", ex);
          case WebClientException ex -> new DirectLlmClientException("Request failed", ex);
          default -> new DirectLlmClientException("Unexpected error", e);
        })
        .publishOn(Schedulers.boundedElastic());
  }

  /**
   * LLM 스트리밍 원본 응답을 context로 전달한다.
   *
   * <p>
   * cite 태그 파싱 없이 원본 {@link ChatCompletionResponse}를 그대로 전달한다.
   * 완료/에러/취소 시 누적된 content, reasoning, toolCalls가 포함된 최종 응답을 생성한다.
   * </p>
   *
   * @param request 요청 정보
   * @param context 워크플로우 context
   */
  public WorkflowContext<ChatCompletionResponse> streamRawToContext(
      final DirectLlmRequest request,
      final WorkflowListener<ChatCompletionResponse> listener) {
    final WorkflowContext<ChatCompletionResponse> context = new WorkflowContext<>(listener);
    final ChatMessageAssembler assembler = new ChatMessageAssembler();
    final ChatCompletionResponse[] lastResponse = { null };

    // 취소 시에도 현재까지 누적된 부분 결과를 반환하도록 콜백 설정
    context.setOnCancel(() -> {
      context.setResult(buildChatResponse(lastResponse[0], assembler, "cancelled"));
      context.emitComplete();
    });

    context.setDisposable(streamRaw(request)
        .subscribe(
            response -> {
              lastResponse[0] = response;
              final var choices = response.getChoices();
              final ResponseMessage delta = (choices == null || choices.isEmpty())
                  ? null
                  : choices.get(0).getDelta();
              assembler.processDelta(delta);
              context.emitNext(response);
            },
            error -> {
              context.setResult(buildChatResponse(lastResponse[0], assembler, "error"));
              context.emitError(error);
            },
            () -> {
              context.setResult(buildChatResponse(lastResponse[0], assembler, "stop"));
              context.emitComplete();
            }));
    return context;
  }

  /**
   * ChatMessageAssembler로부터 최종 ChatCompletionResponse를 생성한다.
   */
  private ChatCompletionResponse buildChatResponse(
      final ChatCompletionResponse lastResponse,
      final ChatMessageAssembler assembler,
      final String finishReason) {
    final ResponseMessage message = assembler.buildFinalMessage();

    final ChatCompletionResponse.ChatCompletionResponseBuilder<?, ?> builder = ChatCompletionResponse.builder()
        .choices(List.of(BaseCompletionResponse.Choice.<ResponseMessage>builder()
            .index(0)
            .message(message)
            .finishReason(finishReason)
            .build()));

    if (lastResponse != null) {
      builder.id(lastResponse.getId())
          .object(lastResponse.getObject())
          .created(lastResponse.getCreated())
          .model(lastResponse.getModel())
          .usage(lastResponse.getUsage());
    }

    return builder.build();
  }

  /**
   * LLM 스트리밍 응답을 context로 전달한다.
   *
   * <p>
   * 스트리밍 중에는 각 토큰마다 {@link WorkflowContext#emitNext}로 개별 응답을 전달하고,
   * 완료/에러/취소 시 {@link WorkflowContext#setResult}로 현재까지 누적된 텍스트와
   * 인용 정보가 포함된 {@link AdvancedCompletionResponse}를 설정한다.
   * </p>
   *
   * @param request 요청 정보
   * @param context 워크플로우 context
   */
  public WorkflowContext<AdvancedCompletionResponse> streamToContext(
      final DirectLlmRequest request,
      final WorkflowListener<AdvancedCompletionResponse> listener) {
    final WorkflowContext<AdvancedCompletionResponse> context = new WorkflowContext<>(listener);
    final CitedMessageAssembler assembler = new CitedMessageAssembler();
    final ChatCompletionResponse[] lastResponse = { null };

    // 취소 시에도 현재까지 누적된 부분 결과를 반환하도록 콜백 설정
    context.setOnCancel(() -> {
      assembler.flush();
      context.setResult(buildAdvancedResponse(
          lastResponse[0], assembler.buildFinalMessage(), "cancelled"));
      context.emitComplete();
    });

    context.setDisposable(streamRaw(request)
        .subscribe(
            response -> {
              lastResponse[0] = response;
              final var choices = response.getChoices();
              if (choices == null || choices.isEmpty()) {
                return;
              }
              final var choice = choices.get(0);
              final String finishReason = choice.getFinishReason();
              final List<CitedMessage> deltas = assembler.processDelta(choice.getDelta());

              // finishReason이 있으면 delta가 비어있어도 emit
              if (finishReason != null) {
                final CitedMessage delta = deltas.isEmpty() ? CitedMessage.builder().build() : deltas.get(0);
                context.emitNext(buildAdvancedResponse(response, delta, finishReason));
              } else {
                for (final CitedMessage delta : deltas) {
                  context.emitNext(buildAdvancedResponse(response, delta, null));
                }
              }
            },
            error -> {
              assembler.flush();
              context.setResult(buildAdvancedResponse(
                  lastResponse[0], assembler.buildFinalMessage(), "error"));
              context.emitComplete();
            },
            () -> {
              assembler.flush();
              context.setResult(buildAdvancedResponse(
                  lastResponse[0], assembler.buildFinalMessage(), "stop"));
              context.emitComplete();
            }));
    return context;
  }

  /**
   * AdvancedCompletionResponse를 생성한다.
   *
   * @param original     원본 응답 (id, model 등 메타정보용, nullable)
   * @param delta        delta 메시지
   * @param finishReason 종료 이유 (스트리밍 중이면 null)
   */
  private AdvancedCompletionResponse buildAdvancedResponse(
      final ChatCompletionResponse original,
      final CitedMessage delta,
      final String finishReason) {
    final var choiceBuilder = BaseCompletionResponse.Choice.<CitedMessage>builder()
        .index(0)
        .delta(delta)
        .finishReason(finishReason);

    final AdvancedCompletionResponse.AdvancedCompletionResponseBuilder<?, ?> builder = AdvancedCompletionResponse
        .builder()
        .choices(List.of(choiceBuilder.build()));

    if (original != null) {
      builder.id(original.getId())
          .object(original.getObject())
          .created(original.getCreated())
          .model(original.getModel())
          .usage(original.getUsage());
    }

    return builder.build();
  }

}
