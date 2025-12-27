package com.saltlux.workflow.core;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saltlux.workflow.core.common.WorkflowContext;
import com.saltlux.workflow.core.common.WorkflowListener;
import com.saltlux.workflow.core.common.WorkflowProcessor;
import com.saltlux.workflow.core.payload.WorkflowResponse;
import com.saltlux.workflow.direct.DirectLlmProcessor;
import com.saltlux.workflow.direct.payload.DirectLlmRequest;
import com.saltlux.workflow.direct.payload.chatcompletion.AdvancedCompletionResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 워크플로우 API 클라이언트.
 *
 * <p>
 * Spring 의존성 없이 직접 생성하여 사용한다.
 * </p>
 */
@Slf4j
public abstract class BaseWorkflowClient {
  protected final WorkflowProcessor processor;
  protected final DirectLlmProcessor llmClient;
  protected final ObjectMapper objectMapper;

  /**
   * WorkflowClient를 생성한다.
   *
   * @param clientBuilder WebClient.Builder 인스턴스
   * @param url           워크플로우 API 베이스 URL
   * @param apiKey        워크플로우 API 키
   */
  protected BaseWorkflowClient(final WebClient.Builder clientBuilder, final String url, final String apiKey) {
    this.objectMapper = new ObjectMapper();
    this.processor = new WorkflowProcessor(clientBuilder, url, apiKey);
    this.llmClient = new DirectLlmProcessor(clientBuilder, objectMapper);
  }

  /**
   * 워크플로우 서버에 단건 요청을 보내고 결과를 반환한다.
   *
   * @param <T>      응답 타입
   * @param flowPath 플로우 경로
   * @param body     요청 body
   * @param typeRef  응답 타입 레퍼런스
   * @return 응답 결과
   */
  public <T> T getResult(final String flowPath, final Object body,
      final ParameterizedTypeReference<WorkflowResponse<T>> typeRef) {
    return processor.getResult(flowPath, body, typeRef);
  }

  /**
   * 워크플로우 서버에 스트리밍 요청을 보내고 Flux를 반환한다.
   *
   * @param <T>      응답 타입
   * @param flowPath 플로우 경로
   * @param body     요청 body
   * @param typeRef  응답 타입 레퍼런스
   * @return 스트리밍 응답 Flux
   */
  public <T> Flux<T> getResultStream(final String flowPath, final Object body,
      final ParameterizedTypeReference<WorkflowResponse<T>> typeRef) {
    return processor.getResultStream(flowPath, body, typeRef);
  }

  /**
   * OpenAI 호환 LLM API에 직접 스트리밍 요청을 보낸다.
   *
   * @param request LLM 요청 (baseUrl, apiKey, body 포함)
   * @param context 워크플로우 context
   */
  public WorkflowContext<AdvancedCompletionResponse> streamLlm(
      final DirectLlmRequest request,
      final WorkflowListener<AdvancedCompletionResponse> listener) {
    return llmClient.streamToContext(request, listener);
  }
}
