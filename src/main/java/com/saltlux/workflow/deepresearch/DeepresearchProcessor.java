package com.saltlux.workflow.deepresearch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.ParameterizedTypeReference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.saltlux.workflow.core.common.WorkflowContext;
import com.saltlux.workflow.core.common.WorkflowListener;
import com.saltlux.workflow.core.common.WorkflowProcessor;
import com.saltlux.workflow.core.payload.WorkflowResponse;
import com.saltlux.workflow.deepresearch.common.FlowPathProperties;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.AnalysisRequest;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.AnalyzeAndPlanRequest;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.GlobalAnalysisResponse;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.IndexLevelAnalysisResponse;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.Sufficiency;
import com.saltlux.workflow.deepresearch.payload.ChatPayloads.ChatRequest;
import com.saltlux.workflow.deepresearch.payload.IntentClassificationResponse;
import com.saltlux.workflow.deepresearch.payload.MakeTitlePayloads.TitleGenerationRequest;
import com.saltlux.workflow.deepresearch.payload.MakeTitlePayloads.TitleGenerationResponse;
import com.saltlux.workflow.deepresearch.payload.ModelInfoPayloads.ModelInfoResponse;
import com.saltlux.workflow.deepresearch.payload.PrecedentPayloads.PrecedentChunk;
import com.saltlux.workflow.deepresearch.payload.PrecedentPayloads.PrecedentFilter;
import com.saltlux.workflow.deepresearch.payload.PrecedentPayloads.PrecedentQuery;
import com.saltlux.workflow.deepresearch.payload.PrecedentPayloads.PrecedentRetrieveRequest;
import com.saltlux.workflow.deepresearch.payload.PrecedentPayloads.PrecedentRetrieveResponse;
import com.saltlux.workflow.deepresearch.payload.QueryExpansionPayloads.QueryExpansionRequest;
import com.saltlux.workflow.deepresearch.payload.QueryExpansionPayloads.QueryExpansionResponse;
import com.saltlux.workflow.deepresearch.payload.SystemPromptPayloads.SystemPromptRequest;
import com.saltlux.workflow.deepresearch.payload.QueryReconstructionResponse;
import com.saltlux.workflow.deepresearch.payload.ReasoningObject;
import com.saltlux.workflow.deepresearch.payload.ScoreInfos.Scored;
import com.saltlux.workflow.deepresearch.payload.SelfQueryResponse;
import com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteChunk;
import com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteFilter;
import com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteQuery;
import com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteRetrieveRequest;
import com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteRetrieveResponse;
import com.saltlux.workflow.deepresearch.payload.messageable.IRetrievalFlow;
import com.saltlux.workflow.deepresearch.payload.messageable.PrecedentRetrievalFlow;
import com.saltlux.workflow.deepresearch.payload.messageable.ResearchResult;
import com.saltlux.workflow.deepresearch.payload.messageable.StatuteRetrievalFlow;
import com.saltlux.workflow.direct.DirectLlmProcessor;
import com.saltlux.workflow.direct.payload.DirectLlmRequest;
import com.saltlux.workflow.direct.payload.chatcompletion.ChatCompletionResponse;
import com.saltlux.workflow.direct.payload.chatcompletion.Message;
import com.saltlux.workflow.direct.payload.messageable.IDocument;
import com.saltlux.workflow.direct.payload.messageable.ResponseMessage;

import lombok.extern.slf4j.Slf4j;
import me.hanju.adapter.transition.TransitionSchema;

/**
 * 심층 연구 워크플로우 서비스.
 *
 * <p>
 * Spring 의존성 없이 직접 생성하여 사용한다.
 * </p>
 */
@Slf4j
public class DeepresearchProcessor {

  private static final int MAX_RETRY = 2;

  /** analysis/plan 태그를 인식하는 스키마 */
  private static final TransitionSchema ANALYSIS_PLAN_SCHEMA = TransitionSchema.root()
      .tag("analysis").tag("plan");
  private static final ParameterizedTypeReference<WorkflowResponse<ModelInfoResponse>> MODEL_INFO_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<IntentClassificationResponse>> INTENT_CLASSIFICATION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<TitleGenerationResponse>> TITLE_GENERATION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<SelfQueryResponse>> SELF_QUERY_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<QueryReconstructionResponse>> QUERY_RECONSTRUCTION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<QueryExpansionResponse>> QUERY_EXPANSION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<StatuteRetrieveResponse>> STATUTE_RETRIEVE_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<PrecedentRetrieveResponse>> PRECEDENT_RETRIEVE_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final TypeReference<IndexLevelAnalysisResponse> INDEX_LEVEL_ANALYSIS_RESPONSE_TYPE = new TypeReference<>() {
  };
  private static final TypeReference<GlobalAnalysisResponse> GLOBAL_ANALYSIS_RESPONSE_TYPE = new TypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<DirectLlmRequest>> DIRECT_LLM_REQUEST_TYPE = new ParameterizedTypeReference<>() {
  };
  private static final ParameterizedTypeReference<WorkflowResponse<String>> SYSTEM_PROMPT_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };

  private final WorkflowProcessor processor;
  private final FlowPathProperties properties;
  private final DirectLlmProcessor llmClient;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  public DeepresearchProcessor(
      final WorkflowProcessor processor,
      final FlowPathProperties properties,
      final DirectLlmProcessor llmClient,
      final com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    this.processor = processor;
    this.properties = properties;
    this.llmClient = llmClient;
    this.objectMapper = objectMapper;
  }

  // ========== 공개 API 메서드 ==========

  /**
   * 시스템 프롬프트를 조회한다.
   *
   * @param type 프롬프트 유형 (예: "deepresearch")
   * @return 시스템 프롬프트 문자열
   */
  public String systemPrompt(final String type) {
    return processor.getResult(
        properties.getSystemPromptPath(),
        SystemPromptRequest.builder().type(type).build(),
        SYSTEM_PROMPT_RESPONSE_TYPE);
  }

  // ========== 내부 플로우 메서드 (package-private for testing) ==========

  SelfQueryResponse selfQuery(final ChatRequest req) {
    return processor.getResult(
        properties.getSelfQueryPath(),
        req,
        SELF_QUERY_RESPONSE_TYPE);
  }

  QueryReconstructionResponse queryReconstruction(final ChatRequest req) {
    return processor.getResult(
        properties.getQueryReconstructionPath(),
        req,
        QUERY_RECONSTRUCTION_RESPONSE_TYPE);
  }

  QueryExpansionResponse queryExpansion(final QueryExpansionRequest req) {
    return processor.getResult(
        properties.getQueryExpansionPath(),
        req,
        QUERY_EXPANSION_RESPONSE_TYPE);
  }

  StatuteRetrieveResponse statuteRetrieve(final StatuteRetrieveRequest req) {
    return processor.getResult(
        properties.getStatuteRetrievePath(),
        req,
        STATUTE_RETRIEVE_RESPONSE_TYPE);
  }

  PrecedentRetrieveResponse precedentRetrieve(
      final PrecedentRetrieveRequest req) {
    return processor.getResult(
        properties.getPrecedentRetrievePath(),
        req,
        PRECEDENT_RETRIEVE_RESPONSE_TYPE);
  }

  WorkflowContext<ReasoningObject<IndexLevelAnalysisResponse>> indexLevelAnalysis(
      final AnalysisRequest req,
      final WorkflowListener<ReasoningObject<IndexLevelAnalysisResponse>> listener) {
    final WorkflowContext<ReasoningObject<IndexLevelAnalysisResponse>> context = new WorkflowContext<>(listener);
    streamToReasoningObject(
        processor.getResult(properties.getIndexLevelAnalysisPath(), req, DIRECT_LLM_REQUEST_TYPE),
        context,
        INDEX_LEVEL_ANALYSIS_RESPONSE_TYPE);
    return context;
  }

  WorkflowContext<ReasoningObject<GlobalAnalysisResponse>> globalLevelAnalysis(
      final AnalysisRequest req,
      final WorkflowListener<ReasoningObject<GlobalAnalysisResponse>> listener) {
    final WorkflowContext<ReasoningObject<GlobalAnalysisResponse>> context = new WorkflowContext<>(listener);
    streamToReasoningObject(
        processor.getResult(properties.getGlobalLevelAnalysisPath(), req, DIRECT_LLM_REQUEST_TYPE),
        context,
        GLOBAL_ANALYSIS_RESPONSE_TYPE);
    return context;
  }

  public WorkflowContext<ReasoningObject<String>> analyzeAndPlan(
      final AnalyzeAndPlanRequest req,
      final WorkflowListener<ReasoningObject<String>> listener) {
    final DirectLlmRequest directRequest = processor.getResult(
        properties.getAnalyzeAndPlanPath(),
        req,
        DIRECT_LLM_REQUEST_TYPE);
    return streamToAnalysisPlan(directRequest, listener);
  }

  // ========== 공개 워크플로우 메서드 ==========

  public ModelInfoResponse models() {
    return processor.getResult(properties.getModelsPath(), Map.of("", ""), MODEL_INFO_RESPONSE_TYPE);
  }

  /**
   * 대화 내용을 기반으로 채팅방 제목을 생성한다.
   *
   * @param req 제목 생성 요청 (사용자 질의, LLM 응답 포함)
   * @return 생성된 제목
   */
  public TitleGenerationResponse titleGeneration(final TitleGenerationRequest req) {
    return processor
        .getResult(
            properties.getTitleGenerationPath(),
            req,
            TITLE_GENERATION_RESPONSE_TYPE);
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
    return processor.getResult(
        properties.getIntentClassificationPath(),
        req,
        INTENT_CLASSIFICATION_RESPONSE_TYPE);
  }

  /**
   * 법률 관련 질의에 대해 법령과 판례를 검색하고 분석하여 응답을 생성하는 심층 연구 워크플로우를 비동기로 실행한다.
   *
   * <p>
   * 워크플로우 진행 단계별로 {@link WorkflowListener#onNext}를 통해 {@link ResearchResult} 이벤트가
   * 전달된다.
   * 각 단계가 완료될 때마다 해당 필드만 채워진 partial 객체가 emit된다.
   * 검색 관련 이벤트는 {@code retrievalFlows} 필드를 통해 전달되며,
   * 각 재시도가 별도의 {@link IRetrievalFlow}로 추가된다.
   * 이벤트는 여러 스레드에서 동시에 호출될 수 있으므로, listener 구현 시 thread-safe하게 작성해야 한다.
   * </p>
   *
   * @param model     사용할 모델
   * @param history   이전 대화 내역
   * @param lastQuery 사용자의 마지막 질의
   * @param listener  워크플로우 이벤트를 수신할 리스너. 여러 스레드에서 동시 호출될 수 있음
   * @return 워크플로우 제어를 위한 context. {@link WorkflowContext#cancel()}로 중단 가능
   */
  public WorkflowContext<ResearchResult> deepresearch(
      final String model,
      final List<Message> history,
      final String lastQuery,
      final WorkflowListener<ResearchResult> listener) {
    final WorkflowContext<ResearchResult> context = new WorkflowContext<>(listener);
    final ResearchResult result = ResearchResult.builder().build();
    result.setError(false);
    context.setResult(result);

    // 취소 시에도 현재까지 누적된 부분 결과를 반환하도록 콜백 설정
    context.setOnCancel(context::emitComplete);

    CompletableFuture.runAsync(() -> {
      try {
        context.checkCompleted();

        final ChatRequest chatRequest = ChatRequest.builder()
            .history(history)
            .lastQuery(lastQuery)
            .build();

        // 1. self-query
        final SelfQueryResponse selfQueryResponse = selfQuery(chatRequest);
        if (selfQueryResponse.getBaseDate() == null) {
          selfQueryResponse.setBaseDate(
              Integer.parseInt(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)));
        }
        context.emitNext(ResearchResult.builder().selfQuery(selfQueryResponse).build());
        result.setSelfQuery(selfQueryResponse);
        context.setResult(result);

        context.checkCompleted();

        // 2. query-reconstruction
        final String searchQuery = queryReconstruction(chatRequest).getSearchQuery();
        context.emitNext(ResearchResult.builder().searchQuery(searchQuery).build());
        result.setSearchQuery(searchQuery);
        context.setResult(result);

        context.checkCompleted();

        // 3. 병렬: 법령/판례 검색
        final AtomicInteger flowIndex = new AtomicInteger(0);
        final CompletableFuture<Void> statuteFuture = CompletableFuture
            .runAsync(() -> executeStatuteRetrieval(
                searchQuery,
                selfQueryResponse.getStatuteFilter(),
                selfQueryResponse.getBaseDate(),
                result,
                flowIndex,
                context));

        final CompletableFuture<Void> precedentFuture = CompletableFuture
            .runAsync(() -> executePrecedentRetrieval(
                searchQuery,
                selfQueryResponse.getPrecedentFilter(),
                selfQueryResponse.getBaseDate(),
                result,
                flowIndex,
                context));
        CompletableFuture.allOf(statuteFuture, precedentFuture).join();

        context.checkCompleted();

        // 4. 글로벌 판단
        log.info("[RETRIEVAL FLOWS] count={}, flows={}", result.getRetrievalFlows().size(), result.getRetrievalFlows());
        for (IRetrievalFlow flow : result.getRetrievalFlows()) {
          log.info("[FLOW] type={}, index={}, docCount={}, docs={}", flow.getClass().getSimpleName(), flow.getIndex(), flow.getDocumentCount(), flow.getDocuments());
        }
        final List<IDocument> allDocs = result.getAllDocuments();
        log.info("[ALL DOCS] count={}, docs={}", allDocs.size(), allDocs);
        final ReasoningObject<GlobalAnalysisResponse> globalAnalysis = globalLevelAnalysis(
            AnalysisRequest.builder().query(searchQuery).documents(allDocs).build(),
            new WorkflowListener<ReasoningObject<GlobalAnalysisResponse>>() {
              StringBuilder reasonBuilder = new StringBuilder();
              Sufficiency sufficiencyChecker = null;

              @Override
              public void onNext(ReasoningObject<GlobalAnalysisResponse> item) {
                ResearchResult delta = ResearchResult.builder().build();
                if (item.getReason() != null && !item.getReason().isBlank()) {
                  delta.setReason(item.getReason());
                  reasonBuilder.append(item.getReason());
                }
                if (item.getData() != null && item.getData().getIsDataSufficient() != null) {
                  delta.setSufficiency(item.getData().getIsDataSufficient());
                  sufficiencyChecker = item.getData().getIsDataSufficient();
                }
                context.emitNext(delta);
              }

              @Override
              public void onError(Throwable e) {
                result.setReason(reasonBuilder.toString());
                result.setSufficiency(sufficiencyChecker);
                result.setError(true);
                context.setResult(result);
                context.emitComplete();
              }

              // complete 전파 안함
              @Override
              public void onCancel() {
                result.setReason(reasonBuilder.toString());
                result.setSufficiency(sufficiencyChecker);
                context.setResult(result);
                context.cancel();
              }

            }).get();

        result.setReason(globalAnalysis.getReason());
        result.setSufficiency(globalAnalysis.getData().getIsDataSufficient());
        context.setResult(result);

        context.checkCompleted();

        // 6. 작성계획
        final AnalyzeAndPlanRequest analyzeReq = AnalyzeAndPlanRequest.builder()
            .query(searchQuery)
            .documents(allDocs)
            .reason(globalAnalysis.getReason())
            .build();
        final ReasoningObject<String> analysisPlan = analyzeAndPlan(
            analyzeReq, new WorkflowListener<ReasoningObject<String>>() {
              StringBuilder sb = new StringBuilder();

              @Override
              public void onNext(ReasoningObject<String> item) {
                ResearchResult delta = ResearchResult.builder().build();
                if (item.getData() != null && !item.getData().isBlank()) {
                  delta.setPlan(item.getData());
                  context.emitNext(delta);
                  sb.append(item.getData());
                }
              }

              @Override
              public void onError(Throwable e) {
                result.setPlan(sb.toString());
                result.setError(true);
                context.setResult(result);
                context.emitComplete();
              }

              // complete 전파 안함
              @Override
              public void onCancel() {
                result.setPlan(sb.toString());
                context.setResult(result);
                context.cancel();
              }
            }).get();

        result.setPlan(analysisPlan.getData());

        context.setResult(result);
        context.emitComplete();
      } catch (CancellationException e) {
        context.setResult(result);
        context.cancel();
        log.debug("Deep research cancelled");
      } catch (Exception e) {
        log.error("Deep research failed", e);
        result.setError(true);
        context.setResult(result);
        context.emitComplete();
      }
    });

    return context;
  }

  /**
   * 법령 검색을 실행하고 재시도를 처리한다.
   * 검색 결과는 실시간으로 result에 누적되고 emit된다.
   *
   * @param searchQuery 검색 쿼리
   * @param filter      법령 필터
   * @param baseDate    기준 날짜
   * @param result      결과 객체 (실시간 누적용)
   * @param flowIndex   flow 인덱스 (병렬 처리용)
   * @param context     워크플로우 컨텍스트
   */
  private void executeStatuteRetrieval(
      final String searchQuery,
      final StatuteFilter filter,
      final Integer baseDate,
      final ResearchResult result,
      final AtomicInteger flowIndex,
      final WorkflowContext<ResearchResult> context) {

    // 0. 쿼리 확장
    List<String> currentQueries = queryExpansion(
        QueryExpansionRequest.builder().query(searchQuery).build()).getQueries();

    for (int attempt = 0; attempt <= MAX_RETRY && !context.isCancelled(); attempt++) {
      int curIndex = flowIndex.getAndIncrement();
      context.emitNext(ResearchResult.builder().retrievalFlows(List.of(StatuteRetrievalFlow.builder()
          .index(curIndex)
          .expandedQueries(new ArrayList<>(currentQueries))
          .build())).build());

      context.checkCompleted();

      // 1. 검색 실행
      final List<StatuteChunk> docs = doStatuteRetrieve(searchQuery, currentQueries, filter, baseDate);
      final StatuteRetrievalFlow searchResultFlow = StatuteRetrievalFlow.builder()
          .index(curIndex)
          .documents(docs)
          .build();
      searchResultFlow.updateDocumentCount();
      context.emitNext(ResearchResult.builder().retrievalFlows(List.of(searchResultFlow)).build());

      context.checkCompleted();

      // 2. 분석
      final ReasoningObject<IndexLevelAnalysisResponse> analysis = indexLevelAnalysis(
          AnalysisRequest.builder()
              .query(searchQuery)
              .documents(docs.stream().map(c -> (IDocument) c).toList())
              .build(),
          new WorkflowListener<ReasoningObject<IndexLevelAnalysisResponse>>() {

            @Override
            public void onNext(ReasoningObject<IndexLevelAnalysisResponse> item) {
              StatuteRetrievalFlow delta = StatuteRetrievalFlow.builder().index(curIndex).build();
              if (item.getReason() != null && !item.getReason().isBlank()) {
                delta.setReason(item.getReason());
              }
              if (item.getData() != null && item.getData().getIsDataSufficient() != null) {
                delta.setSufficiency(item.getData().getIsDataSufficient());
              }
              context.emitNext(ResearchResult.builder().retrievalFlows(List.of(delta)).build());
            }
          }).get();

      // flow 생성해 저장
      final StatuteRetrievalFlow flow = StatuteRetrievalFlow.builder()
          .index(curIndex)
          .expandedQueries(new ArrayList<>(currentQueries))
          .reason(analysis.getReason())
          .sufficiency(analysis.getData().getIsDataSufficient())
          .documents(docs)
          .build();
      flow.updateDocumentCount();

      result.getRetrievalFlows().add(flow);
      context.setResult(result);

      // 충분하면 종료
      if (analysis.getData().getIsDataSufficient() == Sufficiency.pass) {
        break;
      }

      // 다음 시도를 위해 supportedQueries 사용
      final List<String> nextQueries = analysis.getData().getSupportedQueries();
      if (nextQueries == null || nextQueries.isEmpty()) {
        break;
      }
      currentQueries = nextQueries;
    }
  }

  /**
   * 판례 검색을 실행하고 재시도를 처리한다.
   * 검색 결과는 실시간으로 result에 누적되고 emit된다.
   *
   * @param searchQuery 검색 쿼리
   * @param filter      판례 필터
   * @param baseDate    기준 날짜
   * @param result      결과 객체 (실시간 누적용)
   * @param flowIndex   flow 인덱스 (병렬 처리용)
   * @param context     워크플로우 컨텍스트
   */
  private void executePrecedentRetrieval(
      final String searchQuery,
      final PrecedentFilter filter,
      final Integer baseDate,
      final ResearchResult result,
      final AtomicInteger flowIndex,
      final WorkflowContext<ResearchResult> context) {

    List<String> currentQueries = queryExpansion(
        QueryExpansionRequest.builder().query(searchQuery).build()).getQueries();

    for (int attempt = 0; attempt <= MAX_RETRY && !context.isCancelled(); attempt++) {
      final int curIndex = flowIndex.getAndIncrement();

      context.emitNext(ResearchResult.builder().retrievalFlows(List.of(PrecedentRetrievalFlow.builder()
          .index(curIndex)
          .expandedQueries(new ArrayList<>(currentQueries))
          .build())).build());

      context.checkCompleted();

      // 1. 검색 실행
      final List<PrecedentChunk> docs = doPrecedentRetrieve(searchQuery, currentQueries, filter, baseDate);
      final PrecedentRetrievalFlow searchResultFlow = PrecedentRetrievalFlow.builder()
          .index(curIndex)
          .documents(docs)
          .build();
      searchResultFlow.updateDocumentCount();
      context.emitNext(ResearchResult.builder().retrievalFlows(List.of(searchResultFlow)).build());

      context.checkCompleted();

      // 2. 분석
      final ReasoningObject<IndexLevelAnalysisResponse> analysis = indexLevelAnalysis(
          AnalysisRequest.builder()
              .query(searchQuery)
              .documents(docs.stream().map(c -> (IDocument) c).toList())
              .build(),
          new WorkflowListener<ReasoningObject<IndexLevelAnalysisResponse>>() {

            @Override
            public void onNext(ReasoningObject<IndexLevelAnalysisResponse> item) {
              PrecedentRetrievalFlow delta = PrecedentRetrievalFlow.builder().index(curIndex).build();
              if (item.getReason() != null && !item.getReason().isBlank()) {
                delta.setReason(item.getReason());
              }
              if (item.getData() != null && item.getData().getIsDataSufficient() != null) {
                delta.setSufficiency(item.getData().getIsDataSufficient());
              }
              context.emitNext(ResearchResult.builder().retrievalFlows(List.of(delta)).build());
            }
          }).get();

      // flow 생성, 누적
      final PrecedentRetrievalFlow flow = PrecedentRetrievalFlow.builder()
          .index(curIndex)
          .expandedQueries(new ArrayList<>(currentQueries))
          .reason(analysis.getReason())
          .sufficiency(analysis.getData().getIsDataSufficient())
          .documents(docs)
          .build();
      flow.updateDocumentCount();

      result.getRetrievalFlows().add(flow);
      context.setResult(result);

      // 충분하면 종료
      if (analysis.getData().getIsDataSufficient() == Sufficiency.pass) {
        break;
      }

      // 다음 시도를 위해 supportedQueries 사용
      final List<String> nextQueries = analysis.getData().getSupportedQueries();
      if (nextQueries == null || nextQueries.isEmpty()) {
        break;
      }
      currentQueries = nextQueries;
    }
  }

  /**
   * 법령 검색을 실행한다.
   */
  private List<StatuteChunk> doStatuteRetrieve(
      final String representQuery,
      final List<String> queryStrs,
      final StatuteFilter filter,
      final Integer baseDate) {

    final List<StatuteQuery> queries = new ArrayList<>();
    queries.add(StatuteQuery.builder()
        .query(representQuery)
        .filter(filter)
        .baseDate(baseDate)
        .build());
    queries.addAll(queryStrs.stream()
        .map(q -> StatuteQuery.builder()
            .query(q)
            .baseDate(baseDate)
            .build())
        .toList());

    final List<Scored<StatuteChunk>> results = statuteRetrieve(
        StatuteRetrieveRequest.builder()
            .representQueryStr(representQuery)
            .queries(queries)
            .build())
        .getResults();

    return results.stream()
        .map(Scored::getData)
        .toList();
  }

  /**
   * 판례 검색을 실행한다.
   */
  private List<PrecedentChunk> doPrecedentRetrieve(
      final String representQuery,
      final List<String> queryStrs,
      final PrecedentFilter filter,
      final Integer baseDate) {

    final List<PrecedentQuery> queries = new ArrayList<>();
    queries.add(PrecedentQuery.builder()
        .query(representQuery)
        .filter(filter)
        .baseDate(baseDate)
        .build());
    queries.addAll(queryStrs.stream()
        .map(q -> PrecedentQuery.builder()
            .query(q)
            .baseDate(baseDate)
            .build())
        .toList());

    final List<Scored<PrecedentChunk>> results = precedentRetrieve(
        PrecedentRetrieveRequest.builder()
            .representQueryStr(representQuery)
            .queries(queries)
            .build())
        .getResults();

    return results.stream()
        .map(Scored::getData)
        .toList();
  }

  // ========== LLM 스트리밍 처리 메서드 ==========

  /**
   * reasoning 과정이 혼합된 객체를 context를 이용해 스트림 형식으로 전달한다.
   */
  private <T> void streamToReasoningObject(
      final DirectLlmRequest request,
      final WorkflowContext<ReasoningObject<T>> context,
      final TypeReference<T> type) {
    final StringBuilder reasonBuilder = new StringBuilder();
    final StringBuilder contentBuilder = new StringBuilder();
    context.setDisposable(llmClient.streamRaw(request)
        .subscribe(
            item -> {
              ResponseMessage delta = item.getChoices().get(0).getDelta();
              if (delta.getReasoning() != null && !delta.getReasoning().isBlank()) {
                reasonBuilder.append(delta.getReasoning());
                context.emitNext(ReasoningObject.<T>builder().reason(delta.getReasoning()).build());
              }
              if (delta.getContent() != null && !delta.getContent().isBlank()) {
                contentBuilder.append(delta.getContent());
              }
            },
            context::emitError,
            () -> {
              try {
                final ReasoningObject<T> result = ReasoningObject.<T>builder()
                    .data(objectMapper.readValue(contentBuilder.toString(), type))
                    .build();
                context.emitNext(result);
                result.setReason(reasonBuilder.toString());
                context.setResult(result);
                context.emitComplete();
              } catch (Exception e) {
                context.emitError(e);
              }
            }));
  }

  /**
   * plan 태그를 파싱하여 ReasoningObject로 반환한다.
   */
  private WorkflowContext<ReasoningObject<String>> streamToAnalysisPlan(
      final DirectLlmRequest request,
      final WorkflowListener<ReasoningObject<String>> listener) {
    final WorkflowContext<ReasoningObject<String>> context = new WorkflowContext<>(listener);
    final StringBuilder reasonBuilder = new StringBuilder();
    final StringBuilder planBuilder = new StringBuilder();

    context.setDisposable(llmClient.streamRaw(request)
        .subscribe(
            response -> {
              final ResponseMessage delta = extractDelta(response);
              // reasoning 처리
              if (delta != null && delta.getReasoning() != null && !delta.getReasoning().isBlank()) {
                reasonBuilder.append(delta.getReasoning());
                context.emitNext(ReasoningObject.<String>builder()
                    .reason(delta.getReasoning())
                    .build());
              }
              if (delta != null && delta.getContent() != null && !delta.getContent().isBlank()) {
                planBuilder.append(delta.getContent());
                context.emitNext(ReasoningObject.<String>builder()
                    .data(delta.getContent())
                    .build());
              }
            },
            context::emitError,
            () -> {
              final ReasoningObject<String> result = ReasoningObject.<String>builder()
                  .reason(reasonBuilder.toString())
                  .data(planBuilder.toString())
                  .build();
              context.setResult(result);
              context.emitComplete();
            }));

    return context;
  }

  private ResponseMessage extractDelta(final ChatCompletionResponse response) {
    if (response.getChoices() == null || response.getChoices().isEmpty()) {
      return null;
    }
    return response.getChoices().get(0).getDelta();
  }
}
