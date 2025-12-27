package com.saltlux.workflow.deepresearch.payload;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 채팅방 제목 생성 플로우의 요청/응답 DTO.
 */
public final class MakeTitlePayloads {

  private MakeTitlePayloads() {
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class TitleGenerationRequest {

    private String query;
    private String answer;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class TitleGenerationResponse {

    private String title;
  }
}
