package com.saltlux.workflow.deepresearch.payload;

import java.util.ArrayList;
import java.util.List;

import com.saltlux.workflow.direct.payload.chatcompletion.Message;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** 채팅 기반 워크플로우의 공통 요청 DTO */
public final class ChatPayloads {

  private ChatPayloads() {
  }

  /** deepresearch에서의 기본 채팅 요청 객체 */
  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class ChatRequest {

    @Builder.Default
    private List<Message> history = new ArrayList<>();

    private String lastQuery;
  }
}
