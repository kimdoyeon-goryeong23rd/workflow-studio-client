package com.saltlux.workflow.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 워크플로우 실행 이벤트를 수신하는 리스너 인터페이스.
 * <p>
 * 스트리밍 방식으로 워크플로우 결과를 수신하고,
 * 완료/에러/취소 이벤트를 처리할 수 있다.
 * </p>
 *
 * @param <T> 워크플로우 결과 항목 타입
 */
public interface WorkflowListener<T> {
  Logger log = LoggerFactory.getLogger(WorkflowListener.class);

  /**
   * 다음 결과 항목이 발생했을 때 호출된다.
   * <p>
   * 워크플로우 실행 중 스트리밍 방식으로 여러 번 호출될 수 있다.
   * </p>
   *
   * @param item 수신된 결과 항목
   */
  void onNext(final T item);

  /**
   * 워크플로우 실행 중 에러가 발생했을 때 호출된다.
   * <p>
   * 기본 구현은 경고 로그를 출력한다.
   * </p>
   *
   * @param e 발생한 예외
   */
  default void onError(Throwable e) {
    log.warn("Unhandled error while listening: ", e);
  }

  /**
   * 워크플로우가 정상적으로 완료되었을 때 호출된다.
   * <p>
   * 기본 구현은 디버그 로그를 출력한다.
   * </p>
   */
  default void onComplete() {
    log.debug("Workflow completed");
  }

  /**
   * 워크플로우가 취소되었을 때 호출된다.
   * <p>
   * 기본 구현은 디버그 로그를 출력한다.
   * </p>
   */
  default void onCancel() {
    log.debug("Workflow cancelled");
  }
}
