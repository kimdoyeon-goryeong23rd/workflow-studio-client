package com.saltlux.workflow.deepresearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saltlux.workflow.core.common.WorkflowContext;
import com.saltlux.workflow.core.common.WorkflowListener;
import com.saltlux.workflow.core.common.WorkflowProcessor;
import com.saltlux.workflow.deepresearch.common.FlowPathProperties;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.AnalysisRequest;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.AnalyzeAndPlanRequest;
import com.saltlux.workflow.deepresearch.payload.AnalysisPayloads.GlobalAnalysisResponse;
import com.saltlux.workflow.deepresearch.payload.ReasoningObject;
import com.saltlux.workflow.direct.DirectLlmProcessor;
import com.saltlux.workflow.direct.payload.messageable.IDocument;
import com.saltlux.workflow.direct.payload.messageable.SimpleDocument;

import reactor.netty.http.client.HttpClient;

/**
 * DeepresearchProcessor 통합 테스트.
 *
 * <p>
 * globalLevelAnalysis, analyzeAndPlan 등 package-private 메서드를 테스트합니다.
 * </p>
 */
@Tag("integration")
@DisplayName("DeepresearchProcessor 통합 테스트")
class DeepresearchProcessorIntegrationTest {

  private static final String WORKFLOW_URL = System.getenv()
      .getOrDefault("WORKFLOW_URL", "http://211.109.9.48:8050/luxia-workflow-processor");

  private static final String WORKFLOW_API_KEY = System.getenv()
      .getOrDefault("WORKFLOW_API_KEY", "demo");

  private DeepresearchProcessor processor;

  @BeforeEach
  void setUp() {
    WebClient.Builder webClientBuilder = WebClient.builder()
        .clientConnector(
            new ReactorClientHttpConnector(
                HttpClient.create().responseTimeout(Duration.ofMinutes(5))));

    FlowPathProperties properties = FlowPathProperties.builder()
        .modelsPath("lexbase-models")
        .indexLevelAnalysisPath("lexai-local-index-analysis")
        .globalLevelAnalysisPath("lexai-global-analysis")
        .analyzeAndPlanPath("lexai-analyze-plan")
        .build();

    WorkflowProcessor workflowProcessor = new WorkflowProcessor(webClientBuilder, WORKFLOW_URL, WORKFLOW_API_KEY);
    ObjectMapper objectMapper = new ObjectMapper();
    DirectLlmProcessor llmClient = new DirectLlmProcessor(webClientBuilder, objectMapper);
    processor = new DeepresearchProcessor(workflowProcessor, properties, llmClient, objectMapper);
  }

  @Test
  @DisplayName("globalLevelAnalysis - 글로벌 분석 스트리밍 검증")
  void globalLevelAnalysis_shouldStreamValidResponse() throws InterruptedException {
    // given - 테스트용 문서
    String query = "임대차 계약 해지 시 보증금 반환 기한은?";
    List<IDocument> documents = List.of(
        SimpleDocument.builder()
            .id("doc1")
            .title("주택임대차보호법 제4조")
            .content("임대차가 종료된 경우에도 임차인이 보증금을 반환받을 때까지는 임대차관계가 존속하는 것으로 본다.")
            .build(),
        SimpleDocument.builder()
            .id("doc2")
            .title("민법 제536조")
            .content("쌍무계약의 당사자 일방은 상대방이 그 채무이행을 제공할 때까지 자기의 채무이행을 거절할 수 있다.")
            .build());

    AnalysisRequest request = AnalysisRequest.builder()
        .query(query)
        .documents(documents)
        .build();

    StringBuilder reasonBuilder = new StringBuilder();
    AtomicReference<GlobalAnalysisResponse> resultRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> errorRef = new AtomicReference<>();

    // when
    WorkflowContext<ReasoningObject<GlobalAnalysisResponse>> context = processor.globalLevelAnalysis(
        request,
        new WorkflowListener<>() {
          @Override
          public void onNext(ReasoningObject<GlobalAnalysisResponse> item) {
            if (item.getReason() != null) {
              reasonBuilder.append(item.getReason());
              System.out.print(item.getReason());
            }
            if (item.getData() != null) {
              resultRef.set(item.getData());
              System.out.println("\n[DATA] sufficiency: " + item.getData().getIsDataSufficient());
            }
          }

          @Override
          public void onError(Throwable e) {
            System.err.println("\n[ERROR] " + e.getMessage());
            e.printStackTrace();
            errorRef.set(e);
            latch.countDown();
          }

          @Override
          public void onComplete() {
            System.out.println("\n[COMPLETE]");
            latch.countDown();
          }
        });

    // then
    boolean completed = latch.await(3, TimeUnit.MINUTES);
    assertThat(completed).as("글로벌 분석이 완료되어야 함").isTrue();
    assertThat(errorRef.get()).as("에러가 없어야 함").isNull();

    ReasoningObject<GlobalAnalysisResponse> finalResult = context.get();
    assertThat(finalResult).isNotNull();
    assertThat(finalResult.getReason()).as("reason이 있어야 함").isNotBlank();
    assertThat(finalResult.getData()).as("data가 있어야 함").isNotNull();
    assertThat(finalResult.getData().getIsDataSufficient()).as("sufficiency가 있어야 함").isNotNull();

    System.out.println("\n=== globalLevelAnalysis 결과 ===");
    System.out.println("reason 길이: " + finalResult.getReason().length() + "자");
    System.out.println("sufficiency: " + finalResult.getData().getIsDataSufficient());
  }

  @Test
  @DisplayName("analyzeAndPlan - 분석 및 계획 수립 스트리밍 검증")
  void analyzeAndPlan_shouldStreamValidResponse() throws InterruptedException {
    // given - 테스트용 문서와 reason
    String query = "임대차 계약 해지 시 보증금 반환 기한은?";
    List<IDocument> documents = List.of(
        SimpleDocument.builder()
            .id("doc1")
            .title("주택임대차보호법 제4조")
            .content("임대차가 종료된 경우에도 임차인이 보증금을 반환받을 때까지는 임대차관계가 존속하는 것으로 본다.")
            .build(),
        SimpleDocument.builder()
            .id("doc2")
            .title("민법 제536조")
            .content("쌍무계약의 당사자 일방은 상대방이 그 채무이행을 제공할 때까지 자기의 채무이행을 거절할 수 있다.")
            .build());

    String reason = "제공된 문서들은 임대차 계약 종료 시 보증금 반환에 관한 법적 근거를 담고 있습니다.";

    AnalyzeAndPlanRequest request = AnalyzeAndPlanRequest.builder()
        .query(query)
        .documents(documents)
        .reason(reason)
        .build();

    StringBuilder planBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> errorRef = new AtomicReference<>();

    // when
    WorkflowContext<ReasoningObject<String>> context = processor.analyzeAndPlan(
        request,
        new WorkflowListener<>() {
          @Override
          public void onNext(ReasoningObject<String> item) {
            if (item.getReason() != null) {
              System.out.print("[R]" + item.getReason());
            }
            if (item.getData() != null) {
              planBuilder.append(item.getData());
              System.out.print(item.getData());
            }
          }

          @Override
          public void onError(Throwable e) {
            System.err.println("\n[ERROR] " + e.getMessage());
            e.printStackTrace();
            errorRef.set(e);
            latch.countDown();
          }

          @Override
          public void onComplete() {
            System.out.println("\n[COMPLETE]");
            latch.countDown();
          }
        });

    // then
    boolean completed = latch.await(3, TimeUnit.MINUTES);
    assertThat(completed).as("분석 및 계획이 완료되어야 함").isTrue();
    assertThat(errorRef.get()).as("에러가 없어야 함").isNull();

    ReasoningObject<String> finalResult = context.get();
    assertThat(finalResult).isNotNull();
    assertThat(finalResult.getData()).as("plan이 있어야 함").isNotBlank();

    System.out.println("\n=== analyzeAndPlan 결과 ===");
    System.out.println("plan 길이: " + finalResult.getData().length() + "자");
    System.out.println("plan 내용:\n" + finalResult.getData());
  }
}
