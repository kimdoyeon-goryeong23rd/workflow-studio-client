package com.saltlux.workflow.deepresearch.payload.messageable;

import java.util.ArrayList;
import java.util.List;

import com.saltlux.workflow.deepresearch.payload.PrecedentPayloads.PrecedentChunk;
import com.saltlux.workflow.direct.payload.messageable.IDocument;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 판례 검색 결과를 담는 RetrievalFlow 구현체.
 */
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PrecedentRetrievalFlow extends IRetrievalFlow {

  @Builder.Default
  private List<PrecedentChunk> documents = new ArrayList<>();

  @Override
  public List<IDocument> getDocuments() {
    return documents.stream().map(IDocument.class::cast).toList();
  }
}
