package com.saltlux.workflow.deepresearch.payload;

import java.util.List;

import com.saltlux.workflow.deepresearch.payload.ScoreInfos.Scored;
import com.saltlux.workflow.direct.payload.messageable.IDocument;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public final class PrecedentPayloads {

  private PrecedentPayloads() {
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class PrecedentRetrieveRequest {

    private String representQueryStr;
    private List<PrecedentQuery> queries;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class PrecedentRetrieveResponse {

    private List<Scored<PrecedentChunk>> results;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class PrecedentQuery implements IQuery {

    private String query;
    private PrecedentFilter filter;
    private Integer baseDate;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class PrecedentFilter {

    private String caseName;
    private String caseNumber;
    private String court;
    private String caseType;
    private Integer prncYd;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class PrecedentChunk implements IDocument {

    private String id;
    private String caseName;
    private String caseNumber;
    private String content;

    @Override
    public String getId() {
      if (id != null) {
        return id;
      }
      return caseNumber != null ? "precedent-" + caseNumber : null;
    }

    @Override
    public String getTitle() {
      return caseName;
    }

    /**
     * 판례 상세 페이지 URL을 반환한다.
     * <p>
     * 대법원 종합법률정보 기준으로 caseNumber를 사용하여 URL을 생성한다.
     * </p>
     *
     * @return 판례 URL
     */
    @Override
    public String getUrl() {
      if (caseNumber == null || caseNumber.isBlank()) {
        return null;
      }
      return "https://glaw.scourt.go.kr/wsjo/panre/sjo100.do?contId=" + caseNumber;
    }
  }
}
