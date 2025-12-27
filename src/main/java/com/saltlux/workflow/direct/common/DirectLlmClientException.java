package com.saltlux.workflow.direct.common;

/**
 * Direct LLM 클라이언트 처리 중 발생하는 예외.
 */
public class DirectLlmClientException extends RuntimeException {

  public DirectLlmClientException(final String message) {
    super(message);
  }

  public DirectLlmClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
