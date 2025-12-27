package com.saltlux.workflow.core.common;

/**
 * 워크플로우 관련 예외 클래스들을 정의하는 유틸리티 클래스.
 */
public final class WorkflowExceptions {

  private WorkflowExceptions() {
  }

  /**
   * 워크플로우 서버에서 발생한 예외.
   * <p>
   * 서버 측 오류 코드와 메시지를 포함한다.
   * </p>
   */
  public static class WorkflowException extends RuntimeException {

    /** 서버에서 반환한 오류 코드 */
    private final int code;

    public WorkflowException(final int code, final String message) {
      super(message);
      this.code = code;
    }

    public WorkflowException(final int code) {
      super();
      this.code = code;
    }

    public WorkflowException() {
      super();
      this.code = -1;
    }

    public int getCode() {
      return this.code;
    }
  }

  /**
   * 워크플로우 클라이언트에서 발생한 예외.
   * <p>
   * 네트워크 오류, 타임아웃, 인터럽트 등 클라이언트 측 문제를 나타낸다.
   * </p>
   */
  public static class WorkflowClientException extends RuntimeException {

    public WorkflowClientException(String message) {
      super(message);
    }

    public WorkflowClientException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
