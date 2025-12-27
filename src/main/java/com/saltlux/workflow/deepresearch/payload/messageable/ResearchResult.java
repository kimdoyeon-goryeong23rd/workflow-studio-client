package com.saltlux.workflow.deepresearch.payload.messageable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.Sufficiency;
import com.saltlux.workflow.deepresearch.payload.SelfQueryResponse;
import com.saltlux.workflow.direct.payload.messageable.IDocument;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 심층 연구 워크플로우의 결과.
 *
 * <p>
 * 스트리밍 시에는 각 단계가 완료될 때마다 해당 필드만 채워진 partial 객체가 emit됩니다.
 * 검색 관련 이벤트는 {@code retrievalFlows} 배열을 통해 전달되며,
 * 각 재시도가 별도의 {@link IRetrievalFlow} 항목으로 추가됩니다.
 * </p>
 */
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ResearchResult {

  private SelfQueryResponse selfQuery;

  private String searchQuery;

  /** 병렬 검색 결과 */
  @Builder.Default
  private List<IRetrievalFlow> retrievalFlows = new CopyOnWriteArrayList<>();

  /** GlobalAnalysis의 reason */
  private String reason;

  /** GlobalAnalysis의 isDataSufficient */
  private Sufficiency sufficiency;

  private String plan;

  /** 에러로 멈췄으면 true (null이면 JSON에서 생략) */
  private Boolean error;

  /**
   * 모든 retrievalFlows에서 중복을 제거한 문서 목록을 반환한다.
   * <p>
   * 문서의 ID를 기준으로 중복을 판단하며, 순서는 유지된다.
   * </p>
   *
   * @return 중복이 제거된 문서 목록
   */
  @JsonIgnore
  public List<IDocument> getAllDocuments() {
    final Set<String> seenIds = new LinkedHashSet<>();
    final List<IDocument> result = new ArrayList<>();

    for (final IRetrievalFlow flow : retrievalFlows) {
      for (final IDocument doc : flow.getDocuments()) {
        if (doc.getId() != null && seenIds.add(doc.getId())) {
          result.add(doc);
        }
      }
    }

    return result;
  }
}
