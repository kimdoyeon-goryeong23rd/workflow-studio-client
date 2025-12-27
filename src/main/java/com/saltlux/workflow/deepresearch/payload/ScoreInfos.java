package com.saltlux.workflow.deepresearch.payload;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public final class ScoreInfos {

  private ScoreInfos() {
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class Scored<T> {

    private List<OriginInfo> origins;
    private Float fusedScore;
    private Integer fusedRank;
    private Float rerankedScore;
    private Integer rerankedRank;
    private T data;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class OriginInfo {

    private String origin;
    private String query;
    private Float score;
    private Integer rank;
  }
}
