package com.saltlux.workflow.direct.payload.chatcompletion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.saltlux.workflow.direct.DirectLlmProcessor;
import com.saltlux.workflow.direct.payload.DirectLlmRequest;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * OpenAI Chat Completion API 요청 객체.
 *
 * <p>
 * OpenAI 호환 API의 표준 요청 형식을 따릅니다.
 * </p>
 *
 * @see DirectLlmProcessor
 * @see DirectLlmRequest
 */
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseCompletionRequest<T extends IMessage> {

  private String model;

  @lombok.Builder.Default
  private List<T> messages = new ArrayList<>();

  private Double temperature;

  @JsonProperty("top_p")
  private Double topP;

  private Integer n;

  /** 스트리밍 여부 */
  private Boolean stream;

  /** 중단 시퀀스 */
  private List<String> stop;

  /** 최대 토큰 수 */
  @JsonProperty("max_tokens")
  private Integer maxTokens;

  /** 존재 페널티 (-2.0 ~ 2.0) */
  @JsonProperty("presence_penalty")
  private Double presencePenalty;

  /** 빈도 페널티 (-2.0 ~ 2.0) */
  @JsonProperty("frequency_penalty")
  private Double frequencyPenalty;

  /** 토큰별 로그 확률 반환 여부 */
  private Boolean logprobs;

  /** 반환할 상위 로그 확률 개수 */
  @JsonProperty("top_logprobs")
  private Integer topLogprobs;

  /** 사용자 식별자 */
  private String user;

  /** 함수 호출 도구 목록 */
  private List<Map<String, Object>> tools;

  /** 도구 선택 방식 */
  @JsonProperty("tool_choice")
  private Object toolChoice;

  /** 응답 형식 */
  @JsonProperty("response_format")
  private Map<String, Object> responseFormat;

  /** 시드 값 (재현성을 위함) */
  private Integer seed;
}
