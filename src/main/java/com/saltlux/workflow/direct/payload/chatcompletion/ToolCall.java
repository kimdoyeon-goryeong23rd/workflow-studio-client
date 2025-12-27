package com.saltlux.workflow.direct.payload.chatcompletion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Tool call 정보.
 *
 * <p>
 * LLM이 도구 호출을 요청할 때 사용되는 객체입니다.
 * 요청과 응답 양쪽에서 사용됩니다.
 * </p>
 */
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolCall {

  private Integer index;
  private String id;
  private String type;
  private ToolFunction function;
}
