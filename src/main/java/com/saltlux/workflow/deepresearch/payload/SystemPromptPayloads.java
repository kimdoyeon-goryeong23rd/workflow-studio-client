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
 * 시스템 프롬프트 조회 플로우의 요청 DTO.
 */
public final class SystemPromptPayloads {

  private SystemPromptPayloads() {
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class SystemPromptRequest {

    private String type;
  }
}
