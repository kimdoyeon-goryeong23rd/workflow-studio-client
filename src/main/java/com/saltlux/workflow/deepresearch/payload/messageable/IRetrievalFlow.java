package com.saltlux.workflow.deepresearch.payload.messageable;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.Sufficiency;
import com.saltlux.workflow.direct.payload.messageable.IDocument;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 개별 검색 시도 결과.
 *
 * <p>
 * 각 재시도가 별도의 RetrievalFlow로 기록됩니다.
 * sufficiency가 fail이면 supportedQueries를 다음 시도의 expandedQueries로 사용합니다.
 * </p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StatuteRetrievalFlow.class, name = "statute"),
    @JsonSubTypes.Type(value = PrecedentRetrievalFlow.class, name = "precedent")
})
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class IRetrievalFlow {

  /** 배열 내 순서 */
  private int index;

  /** 해당 시도에서 사용한 쿼리들 */
  @Builder.Default
  private List<String> expandedQueries = new ArrayList<>();

  /** 실제 검색 결과 문서 (내부용, JSON 직렬화 제외) */
  @JsonIgnore
  public abstract List<IDocument> getDocuments();

  /** 검색된 문서 수 (null이면 JSON에서 생략) */
  private Integer documentCount;

  /** 문서 수를 업데이트한다. */
  public void updateDocumentCount() {
    this.documentCount = getDocuments().size();
  }

  /** IndexLevelAnalysis의 reason */
  private String reason;

  /** IndexLevelAnalysis의 isDataSufficient */
  private Sufficiency sufficiency;
}
