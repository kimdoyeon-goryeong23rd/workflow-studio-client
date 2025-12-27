package com.saltlux.workflow;

import java.util.List;

import org.springframework.web.reactive.function.client.WebClient;

import com.saltlux.workflow.core.BaseWorkflowClient;
import com.saltlux.workflow.core.common.WorkflowContext;
import com.saltlux.workflow.core.common.WorkflowListener;
import com.saltlux.workflow.deepresearch.DeepresearchProcessor;
import com.saltlux.workflow.deepresearch.common.FlowPathProperties;
import com.saltlux.workflow.deepresearch.payload.ChatPayloads.ChatRequest;
import com.saltlux.workflow.deepresearch.payload.IntentClassificationResponse;
import com.saltlux.workflow.deepresearch.payload.MakeTitlePayloads.TitleGenerationRequest;
import com.saltlux.workflow.deepresearch.payload.MakeTitlePayloads.TitleGenerationResponse;
import com.saltlux.workflow.deepresearch.payload.ModelInfoPayloads.ModelInfoResponse;
import com.saltlux.workflow.deepresearch.payload.messageable.ResearchResult;
import com.saltlux.workflow.direct.payload.chatcompletion.Message;

public class WorkflowClient extends BaseWorkflowClient {
  private final DeepresearchProcessor deepResearchService;

  public WorkflowClient(final WebClient.Builder clientBuilder, final String url, final String apiKey,
      final FlowPathProperties properties) {
    super(clientBuilder, url, apiKey);
    this.deepResearchService = new DeepresearchProcessor(processor, properties, llmClient, objectMapper);
  }

  public ModelInfoResponse models() {
    return deepResearchService.models();
  }

  /**
   * 시스템 프롬프트를 조회한다.
   *
   * @param type 프롬프트 유형 (예: "deepresearch")
   * @return 시스템 프롬프트 문자열
   */
  public String systemPrompt(final String type) {
    return deepResearchService.systemPrompt(type);
  }

  /**
   * 대화 내용을 기반으로 채팅방 제목을 생성한다.
   *
   * @param req 제목 생성 요청 (사용자 질의, LLM 응답 포함)
   * @return 생성된 제목
   */
  public TitleGenerationResponse titleGeneration(final TitleGenerationRequest req) {
    return deepResearchService.titleGeneration(req);
  }

  /**
   * 사용자 질의의 의도를 분류한다.
   *
   * @param req 요청 객체
   *            <ul>
   *            <li>{@code history} - 이전 대화 이력 ({@link Message} 리스트)</li>
   *            <li>{@code lastQuery} - 현재 사용자 질의 (원문 그대로)</li>
   *            </ul>
   * @return {@link IntentClassificationResponse} 의도 분류 결과
   *         (isSummary, isTransition, isChecklist, isSmalltalk, isSearch)
   */
  public IntentClassificationResponse intentClassification(
      final ChatRequest req) {
    return deepResearchService.intentClassification(req);
  }

  /**
   * 법률 관련 질의에 대해 법령과 판례를 검색하고 분석하여 응답을 생성하는 심층 연구 워크플로우를 비동기로 실행한다.
   *
   * <p>
   * 워크플로우 진행 단계별로 {@link WorkflowListener#onNext}를 통해 {@link ResearchResult} 이벤트가
   * 전달된다.
   * 이벤트는 여러 스레드에서 동시에 호출될 수 있으므로, listener 구현 시 thread-safe하게 작성해야 한다.
   * </p>
   *
   * @param model     사용할 모델
   * @param history   이전 대화 내역
   * @param lastQuery 사용자의 마지막 질의
   * @param listener  워크플로우 이벤트를 수신할 리스너. 여러 스레드에서 동시 호출될 수 있음
   * @return 워크플로우 제어를 위한 context. {@link WorkflowContext#cancel()}로 중단 가능
   * @see DeepresearchProcessor#deepresearch(String, List, String,
   *      WorkflowListener)
   */
  public WorkflowContext<ResearchResult> deepresearch(final String model, final List<Message> history,
      final String lastQuery, final WorkflowListener<ResearchResult> listener) {
    return deepResearchService.deepresearch(model, history, lastQuery, listener);
  }

}
