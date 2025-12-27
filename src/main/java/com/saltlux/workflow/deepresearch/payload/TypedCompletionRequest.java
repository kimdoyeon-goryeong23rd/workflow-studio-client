package com.saltlux.workflow.deepresearch.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saltlux.workflow.direct.payload.chatcompletion.Message;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 워크플로우 서버용 타입이 지정된 채팅 요청 객체.
 *
 * <p>
 * 워크플로우 서버에서 시스템 프롬프트 종류를 지정할 때 사용합니다.
 * </p>
 */
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class TypedCompletionRequest {

  /** 사용할 LLM 모델 이름 */
  private String model;

  /** 사용할 시스템 프롬프트 종류 */
  private String type;

  @Builder.Default
  private List<Message> history = new ArrayList<>();

  private String lastQuery;

  private Map<String, Object> tools;
}
