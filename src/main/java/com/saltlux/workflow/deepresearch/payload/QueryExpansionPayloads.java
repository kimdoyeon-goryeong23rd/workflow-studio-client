package com.saltlux.workflow.deepresearch.payload;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public final class QueryExpansionPayloads {

  private QueryExpansionPayloads() {
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class QueryExpansionRequest {

    private String query;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class QueryExpansionResponse {

    @Builder.Default
    private List<String> queries = new ArrayList<>();
  }
}
