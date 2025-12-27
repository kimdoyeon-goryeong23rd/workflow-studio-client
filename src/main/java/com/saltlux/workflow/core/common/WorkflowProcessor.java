package com.saltlux.workflow.core.common;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.CodecException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import com.saltlux.workflow.core.common.WorkflowExceptions.WorkflowClientException;
import com.saltlux.workflow.core.common.WorkflowExceptions.WorkflowException;
import com.saltlux.workflow.core.payload.WorkflowResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * 워크플로우 서버와의 HTTP 통신을 처리하는 클래스.
 *
 * <p>
 * Spring 의존성 없이 직접 생성하여 사용한다.
 * </p>
 *
 * <pre>{@code
 * WorkflowProcessor processor = new WorkflowProcessor(
 *     webClientBuilder,
 *     workflowUrl,
 *     workflowApiKey);
 * }</pre>
 */
public class WorkflowProcessor {

  private final WebClient client;

  public WorkflowProcessor(
      final WebClient.Builder clientBuilder,
      final String url,
      final String apiKey) {
    this.client = clientBuilder
        .baseUrl(url + "/api/flow/")
        .defaultHeaders(headers -> headers.setBearerAuth(apiKey))
        .build();
  }

  /**
   * 워크플로우 응답을 검증하고 결과를 반환한다.
   *
   * @param resp 워크플로우 응답
   * @param <T>  결과 타입
   * @return 검증된 결과
   * @throws WorkflowException 응답이 null이거나 결과가 없는 경우
   */
  private <T> T validateWorkflowResponse(final WorkflowResponse<T> resp) {
    if (resp == null) {
      throw new WorkflowException(-1, "Empty response");
    } else if (resp.getResult() == null) {
      throw new WorkflowException(resp.getCode(), resp.getMessage());
    } else {
      return resp.getResult();
    }
  }

  /**
   * 동기 방식으로 워크플로우를 실행하고 결과를 반환한다.
   *
   * @param flowPath  플로우 경로
   * @param bodyValue 요청 본문
   * @param typeRef   응답 타입 참조
   * @param <T>       결과 타입
   * @return 워크플로우 실행 결과
   * @throws WorkflowException       서버 측 오류
   * @throws WorkflowClientException 클라이언트 측 오류
   */
  public <T> T getResult(
      final String flowPath,
      final Object bodyValue,
      final ParameterizedTypeReference<WorkflowResponse<T>> typeRef) {
    try {
      final WorkflowResponse<T> response = this.client.post()
          .uri(flowPath)
          .bodyValue(bodyValue)
          .retrieve()
          .bodyToMono(typeRef)
          .block();
      return validateWorkflowResponse(response);
    } catch (WorkflowException e) {
      throw e;
    } catch (CodecException e) {
      throw new WorkflowClientException("Failed to serialize/deserialize", e);
    } catch (WebClientException e) {
      throw new WorkflowClientException("Request failed", e);
    }
  }

  /**
   * 스트리밍 방식으로 워크플로우를 실행하고 결과 Flux를 반환한다.
   *
   * @param flowPath  플로우 경로
   * @param bodyValue 요청 본문
   * @param typeRef   응답 타입 참조
   * @param <T>       결과 타입
   * @return 스트리밍 결과 Flux
   */
  public <T> Flux<T> getResultStream(
      final String flowPath,
      final Object bodyValue,
      final ParameterizedTypeReference<WorkflowResponse<T>> typeRef) {
    return this.client.post()
        .uri("{flowPath}/stream", flowPath)
        .bodyValue(bodyValue)
        .retrieve()
        .bodyToFlux(typeRef)
        .map(this::validateWorkflowResponse)
        .onErrorMap(e -> switch (e) {
          case WorkflowException we -> we;
          case CodecException ce -> new WorkflowClientException("Failed to serialize/deserialize", ce);
          case WebClientException wce -> new WorkflowClientException("Request failed", wce);
          default -> new WorkflowClientException("Unexpected error", e);
        })
        .publishOn(Schedulers.boundedElastic());
  }
}
