package com.saltlux.workflow.direct.payload;

import com.saltlux.workflow.direct.DirectLlmProcessor;
import com.saltlux.workflow.direct.payload.chatcompletion.AdvancedCompletionRequest;
import com.saltlux.workflow.direct.payload.messageable.AttachedMessage;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * LLM 직접 호출 요청 DTO.
 *
 * <p>
 * 워크플로우에서 반환된 정보를 기반으로 {@link DirectLlmProcessor}가 직접 API를 호출할 때 사용됩니다.
 * </p>
 */
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class DirectLlmRequest {

  /**
   * API 베이스 URL (예: "http://vllm-server:8000").
   */
  private String baseUrl;

  /**
   * API 키 (선택사항, null이거나 blank면 Authorization 헤더 생략).
   */
  private String apiKey;

  /**
   * Chat Completion 요청 body.
   * <p>
   * messages에 {@link AttachedMessage}를 포함하여 문서를 첨부할 수 있습니다.
   * </p>
   */
  private AdvancedCompletionRequest body;
}
