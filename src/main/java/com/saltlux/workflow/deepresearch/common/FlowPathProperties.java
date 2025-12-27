package com.saltlux.workflow.deepresearch.common;

import lombok.Builder;
import lombok.Getter;

/**
 * 워크플로우 플로우 경로 설정.
 *
 * <p>
 * Spring 의존성 없이 직접 생성하여 사용한다.
 * </p>
 */
@Getter
@Builder
public class FlowPathProperties {

  @Builder.Default
  private final String statuteRetrievePath = "lexai-statute-retrieve";
  @Builder.Default
  private final String precedentRetrievePath = "lexai-precedent-retrieve";

  @Builder.Default
  private final String contractRetrievePath = "nipa-contract-retrieve-new";
  @Builder.Default
  private final String contractAnswerPath = "nipa-contract-answer-new";

  // lexai simple
  @Builder.Default
  private final String modelsPath = "lexbase-models-prod";
  @Builder.Default
  private final String intentClassificationPath = "lexai-intent-classification";
  @Builder.Default
  private final String titleGenerationPath = "lexai-title-generation";

  // lexai deepresearch
  @Builder.Default
  private final String selfQueryPath = "lexai-self-query";
  @Builder.Default
  private final String queryReconstructionPath = "lexai-query-reconstruction";
  @Builder.Default
  private final String queryExpansionPath = "lexai-query-expansion";
  @Builder.Default
  private final String indexLevelAnalysisPath = "lexai-local-index-analysis-prod";
  @Builder.Default
  private final String globalLevelAnalysisPath = "lexai-global-analysis-prod";
  @Builder.Default
  private String analyzeAndPlanPath = "lexai-analyze-plan-prod";
  // @Builder.Default
  // private final String deepresearchAnswerPath =
  // "lexai-deepresearch-answer-prod";

  @Builder.Default
  private String systemPromptPath = "lexbase-system-prompt";
}
