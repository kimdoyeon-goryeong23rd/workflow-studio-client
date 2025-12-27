package com.saltlux.workflow.deepresearch.payload;

import java.util.ArrayList;
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

public final class StatutePayloads {

  private StatutePayloads() {
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class StatuteRetrieveRequest {

    private String representQueryStr;

    @Builder.Default
    private List<StatuteQuery> queries = new ArrayList<>();
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class StatuteRetrieveResponse {

    @Builder.Default
    private List<Scored<StatuteChunk>> results = new ArrayList<>();
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class StatuteChunk implements IDocument {

    private Integer mst;
    private Integer efYd;
    private Integer key;

    private String docId;
    private Integer chunkNo;

    private Integer no;
    private Integer brNo;
    private String title;
    private String rrCls;
    private String content;

    private Integer lsId;
    private String lsNm;
    private Integer ancNm;
    private Integer ancYd;
    private List<String> knd;
    private List<String> org;

    @Override
    public String getId() {
      if (docId != null) {
        return docId;
      }
      if (mst != null) {
        return "statute-" + mst + "-" + efYd + "-" + no;
      }
      return null;
    }

    /**
     * 법령 상세 페이지 URL을 반환한다.
     * <p>
     * 법제처 국가법령정보센터 기준으로 mst와 efYd를 사용하여 URL을 생성한다.
     * </p>
     *
     * @return 법령 URL
     */
    @Override
    public String getUrl() {
      if (mst == null) {
        return null;
      }
      final StringBuilder sb = new StringBuilder();
      sb.append("https://www.law.go.kr/lsSc.do?menuId=1&subMenuId=15&tabMenuId=81&query=");
      sb.append("&dt=20201117&section=&eventId=");
      sb.append("&OC=dnjsdms60&ancYnChk=0&ancYd=&lsiSeq=").append(mst);
      if (efYd != null) {
        sb.append("&efYd=").append(efYd);
      }
      return sb.toString();
    }
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class StatuteQuery implements IQuery {

    private String query;
    private StatuteFilter filter;
    private Integer baseDate;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  @ToString
  public static class StatuteFilter {

    private Integer no;
    private Integer brNo;
    private String title;
    private String org;
    private String knd;
    private String rrCls;
    private Integer ancYd;
    private Integer ancNo;
    private Integer efYd;
  }
}
