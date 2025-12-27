package com.saltlux.workflow.deepresearch.payload;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** 의도 분류 플로우의 응답 DTO */
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class IntentClassificationResponse {

  @JsonProperty("isSummary")
  @JsonAlias({ "is_summary", "isSummary" })
  private boolean isSummary;

  @JsonProperty("isTransition")
  @JsonAlias({ "is_transition", "isTransition" })
  private boolean isTransition;

  @JsonProperty("isSmalltalk")
  @JsonAlias({ "is_smalltalk", "isSmalltalk" })
  private boolean isSmalltalk;

  @JsonProperty("isSearch")
  @JsonAlias({ "is_search", "isSearch" })
  private boolean isSearch;
}
