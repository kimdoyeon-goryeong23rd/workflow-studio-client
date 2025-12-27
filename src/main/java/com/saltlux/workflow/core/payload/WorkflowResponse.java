package com.saltlux.workflow.core.payload;

import com.saltlux.workflow.core.common.WorkflowExceptions.WorkflowException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 워크플로우 서버 응답을 나타내는 클래스.
 * <p>
 * 응답에는 오류 코드, 메시지, 결과가 포함된다.
 * {@link #result}가 null인 경우 오류 응답이며,
 * {@link #code}와 {@link #message}를 통해 {@link WorkflowException}으로 변환된다.
 * </p>
 *
 * @param <T> 결과 타입
 * @see WorkflowException
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class WorkflowResponse<T> {

  /**
   * 응답 코드.
   * <p>
   * 오류 발생 시 {@link WorkflowException#getCode()}로 전달된다.
   * </p>
   */
  private int code;

  /**
   * 응답 메시지.
   * <p>
   * 오류 발생 시 {@link WorkflowException#getMessage()}로 전달된다.
   * </p>
   */
  private String message;

  /**
   * 워크플로우 실행 결과.
   * <p>
   * null인 경우 오류 응답으로 간주된다.
   * </p>
   */
  private T result;
}
