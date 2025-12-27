package com.saltlux.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saltlux.workflow.core.common.WorkflowContext;
import com.saltlux.workflow.core.common.WorkflowListener;
import com.saltlux.workflow.deepresearch.common.FlowPathProperties;
import com.saltlux.workflow.deepresearch.payload.ChatPayloads.ChatRequest;
import com.saltlux.workflow.deepresearch.payload.IntentClassificationResponse;
import com.saltlux.workflow.deepresearch.payload.MakeTitlePayloads.TitleGenerationRequest;
import com.saltlux.workflow.deepresearch.payload.MakeTitlePayloads.TitleGenerationResponse;
import com.saltlux.workflow.deepresearch.payload.ModelInfoPayloads.ModelInfoResponse;
import com.saltlux.workflow.deepresearch.payload.messageable.ResearchResult;
import com.saltlux.workflow.deepresearch.payload.messageable.ResearchedMessage;
import com.saltlux.workflow.deepresearch.payload.messageable.StatuteRetrievalFlow;
import com.saltlux.workflow.direct.payload.DirectLlmRequest;
import com.saltlux.workflow.direct.payload.chatcompletion.AdvancedCompletionRequest;
import com.saltlux.workflow.direct.payload.chatcompletion.AdvancedCompletionResponse;
import com.saltlux.workflow.direct.payload.chatcompletion.Message;
import com.saltlux.workflow.direct.payload.messageable.CitedMessage;
import com.saltlux.workflow.direct.payload.messageable.IMessageable;

import reactor.netty.http.client.HttpClient;

/**
 * WorkflowClient 통합 테스트.
 *
 * <p>
 * 실제 워크플로우 서버를 호출하여 플로우를 테스트합니다.
 * 빌드 시에는 제외되며, 수동으로 실행해야 합니다.
 * </p>
 *
 * <p>
 * 실행 방법:
 *
 * <pre>
 * WORKFLOW_URL=http://your-workflow-server:8050/luxia-workflow-processor \
 * WORKFLOW_API_KEY=your-api-key \
 * ./gradlew test --tests "*.WorkflowClientIntegrationTest" -Pintegration
 * </pre>
 * </p>
 */
@Tag("integration")
@DisplayName("WorkflowClient 통합 테스트")
class WorkflowClientIntegrationTest {

  private static final String WORKFLOW_URL = System.getenv()
      .getOrDefault("WORKFLOW_URL", "http://211.109.9.48:8050/luxia-workflow-processor");

  private static final String WORKFLOW_API_KEY = System.getenv()
      .getOrDefault("WORKFLOW_API_KEY", "demo");

  private static final ObjectMapper JSON = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

  private WorkflowClient client;

  @BeforeEach
  void setUp() {
    WebClient.Builder webClientBuilder = WebClient.builder()
        .clientConnector(
            new ReactorClientHttpConnector(
                HttpClient.create().responseTimeout(Duration.ofMinutes(10))));

    FlowPathProperties properties = FlowPathProperties.builder()
        .modelsPath("lexbase-models")
        .indexLevelAnalysisPath("lexai-local-index-analysis")
        .globalLevelAnalysisPath("lexai-global-analysis")
        .analyzeAndPlanPath("lexai-analyze-plan")
        .build();

    client = new WorkflowClient(webClientBuilder, WORKFLOW_URL, WORKFLOW_API_KEY, properties);
  }

  @Test
  @DisplayName("모델 목록 조회")
  void models_shouldReturnModelList() {
    // when
    ModelInfoResponse response = client.models();

    // then
    assertThat(response).isNotNull();
    assertThat(response.getModels()).isNotNull();

    System.out.println("=== 모델 목록 ===");
    response.getModels().forEach(model -> System.out.println("- " + model.getName() + " (" + model.getAbbr() + ")"));
  }

  @Test
  @DisplayName("시스템 프롬프트 조회 - deepresearch 타입")
  void systemPrompt_shouldReturnDeepresearchPrompt() {
    // when
    String prompt = client.systemPrompt("deepresearch");

    // then
    assertThat(prompt).isNotNull().isNotBlank();

    System.out.println("=== 시스템 프롬프트 (deepresearch) ===");
    System.out.println(prompt);
  }

  @Test
  @DisplayName("의도 분류 - 정상 실행 및 응답 형식 검증")
  void intentClassification_shouldReturnValidResponse() {
    // given
    ChatRequest request = ChatRequest.builder()
        .history(
            List.of(
                Message.builder().role("user").content("안녕하세요").build(),
                Message.builder()
                    .role("assistant")
                    .content("안녕하세요! 무엇을 도와드릴까요?")
                    .build()))
        .lastQuery("임대차 계약 해지 시 보증금 반환 기한은 어떻게 되나요?")
        .build();

    // when
    IntentClassificationResponse response = client.intentClassification(request);

    // then
    assertThat(response).isNotNull();

    System.out.println("=== 의도 분류 결과 ===");
    System.out.println("질의: " + request.getLastQuery());
    System.out.println("isSummary: " + response.isSummary());
    System.out.println("isTransition: " + response.isTransition());
    System.out.println("isSmalltalk: " + response.isSmalltalk());
    System.out.println("isSearch: " + response.isSearch());
  }

  @Test
  @DisplayName("제목 생성 - 정상 실행 및 응답 형식 검증")
  void titleGeneration_shouldReturnValidResponse() {
    // given
    TitleGenerationRequest request = TitleGenerationRequest.builder()
        .query("임대차 계약 해지 시 보증금 반환 기한은 어떻게 되나요?")
        .answer("주택임대차보호법에 따르면, 임대차가 종료된 경우 임대인은 보증금을 반환해야 합니다.")
        .build();

    // when
    TitleGenerationResponse response = client.titleGeneration(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getTitle()).isNotNull().isNotBlank();

    System.out.println("=== 제목 생성 결과 ===");
    System.out.println("사용자 질의: " + request.getQuery());
    System.out.println("생성된 제목: " + response.getTitle());
  }

  @Test
  @DisplayName("DeepResearch - 심층 연구 워크플로우 스트리밍 검증")
  void deepResearch_shouldStreamValidResponse() throws InterruptedException {
    // given
    List<Message> history = List.of(
        Message.builder().role("user").content("임대차 계약에 대해 알려주세요").build(),
        Message.builder().role("assistant").content("네, 임대차 계약에 대해 설명드리겠습니다.").build());
    String lastQuery = "임대차 계약 해지 시 보증금 반환 기한은 어떻게 되나요?";

    List<ResearchResult> receivedEvents = new ArrayList<>();
    AtomicReference<Throwable> errorRef = new AtomicReference<>();
    CountDownLatch completeLatch = new CountDownLatch(1);

    WorkflowListener<ResearchResult> listener = new WorkflowListener<>() {
      @Override
      public void onNext(ResearchResult item) {
        receivedEvents.add(item);
        try {
          System.out.println(JSON.writeValueAsString(item));
        } catch (Exception e) {
          System.out.println("[JSON ERROR] " + e.getMessage());
        }
      }

      @Override
      public void onError(Throwable e) {
        System.err.println("[ERROR] " + e.getMessage());
        e.printStackTrace();
        errorRef.set(e);
        completeLatch.countDown();
      }

      @Override
      public void onComplete() {
        System.out.println("[COMPLETE]");
        completeLatch.countDown();
      }

      @Override
      public void onCancel() {
        System.out.println("[CANCEL]");
        completeLatch.countDown();
      }
    };

    // when
    WorkflowContext<ResearchResult> context = client.deepresearch(
        "luxia3-deep-32b-0901-Q",
        history,
        lastQuery,
        listener);

    // then - 5분 타임아웃
    boolean completed = completeLatch.await(5, TimeUnit.MINUTES);

    assertThat(completed).as("워크플로우가 타임아웃 내에 완료되어야 함").isTrue();

    if (errorRef.get() != null) {
      System.err.println("=== 에러 발생 ===");
      errorRef.get().printStackTrace();
    }

    assertThat(errorRef.get()).as("에러가 없어야 함").isNull();
    assertThat(receivedEvents).as("이벤트가 수신되어야 함").isNotEmpty();

    System.out.println("=== DeepResearch 최종 결과 ===");
    try {
      System.out.println(JSON.writeValueAsString(context.get()));
    } catch (Exception e) {
      System.out.println("[JSON ERROR] " + e.getMessage());
    }
  }

  @Test
  @DisplayName("DeepResearch - 취소 테스트")
  void deepResearch_shouldHandleCancellation() throws InterruptedException {
    // given
    List<Message> history = List.of();
    String lastQuery = "근로기준법에서 정하는 연차휴가 일수는?";

    List<ResearchResult> receivedEvents = new ArrayList<>();
    CountDownLatch completeLatch = new CountDownLatch(1);

    WorkflowListener<ResearchResult> listener = new WorkflowListener<>() {
      @Override
      public void onNext(ResearchResult item) {
        receivedEvents.add(item);
        try {
          System.out.println(JSON.writeValueAsString(item));
        } catch (Exception e) {
          System.out.println("[JSON ERROR] " + e.getMessage());
        }
      }

      @Override
      public void onComplete() {
        System.out.println("[COMPLETE]");
        completeLatch.countDown();
      }

      @Override
      public void onCancel() {
        System.out.println("[CANCEL]");
        completeLatch.countDown();
      }
    };

    // when
    WorkflowContext<ResearchResult> context = client.deepresearch(
        "luxia3-deep-32b-0901-Q",
        history,
        lastQuery,
        listener);

    // 10초 후 취소
    Thread.sleep(10000);
    context.cancel();

    // then
    boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
    assertThat(completed).isTrue();

    // 취소 시점까지의 결과가 있어야 함
    assertThat(context.get()).isNotNull();

    System.out.println("=== 취소 후 최종 결과 ===");
    try {
      System.out.println(JSON.writeValueAsString(context.get()));
    } catch (Exception e) {
      System.out.println("[JSON ERROR] " + e.getMessage());
    }
  }

  @Test
  @DisplayName("streamLlm - ResearchedMessage를 포함한 LLM 스트리밍 응답 검증")
  void streamLlm_shouldStreamResponseWithResearchedMessage() throws InterruptedException {
    // given - 더 많은 문서와 상세한 질문
    String lastQuery = """
        임대차 계약 해지 시 보증금 반환 기한에 관한 질문입니다.
        임대인이 보증금 반환을 미루고 있는데, 관련 법조문을 인용하여 간단히 설명해주세요.
        """;

    // 시스템 프롬프트 - cite 태그 사용법 안내
    String systemPrompt = """
        당신은 법률 전문 AI 어시스턴트입니다.
        사용자의 질문에 답변할 때 반드시 제공된 여러 문서들을 참조하여 답변하세요.

        <document>의 content을 근거로 하거나 <document>의 content에서 인용할 때, 해당 문장은 다음과 같은 인용 형식을 사용하여야 합니다.
        인용 형식: <cite><id>{{id1,id2,...}}</id>{{해당 문장}}</cite>
        """;

    // 시스템 메시지 생성
    ResearchedMessage systemMessage = ResearchedMessage.builder()
        .role("system")
        .content(systemPrompt)
        .build();

    // 샘플 문서가 포함된 ResearchResult 생성 - 더 많은 문서 추가
    ResearchResult sampleResult = ResearchResult.builder()
        .searchQuery(lastQuery)
        .reason("임대차 계약 해지 시 보증금 반환 기한에 관한 법적 근거를 분석합니다.")
        .plan("1. 주택임대차보호법 조문 분석\n2. 민법 관련 조항 검토\n3. 결론 도출")
        .retrievalFlows(List.of(
            StatuteRetrievalFlow.builder()
                .index(0)
                .documents(List.of(
                    com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteChunk.builder()
                        .docId("statute-1")
                        .title("주택임대차보호법 제4조 (임대차기간 등)")
                        .content(
                            "① 기간을 정하지 아니하거나 2년 미만으로 정한 임대차는 그 기간을 2년으로 본다. 다만, 임차인은 2년 미만으로 정한 기간이 유효함을 주장할 수 있다. ② 임대차가 종료된 경우에도 임차인이 보증금을 반환받을 때까지는 임대차관계가 존속하는 것으로 본다.")
                        .build(),
                    com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteChunk.builder()
                        .docId("statute-2")
                        .title("주택임대차보호법 제6조 (계약의 갱신)")
                        .content(
                            "① 임대인이 임대차기간이 끝나기 6개월 전부터 2개월 전까지의 기간에 임차인에게 갱신거절의 통지를 하지 아니하거나 계약조건을 변경하지 아니하면 갱신하지 아니한다는 뜻의 통지를 하지 아니한 경우에는 그 기간이 끝난 때에 전 임대차와 동일한 조건으로 다시 임대차한 것으로 본다.")
                        .build(),
                    com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteChunk.builder()
                        .docId("statute-3")
                        .title("민법 제536조 (동시이행의 항변권)")
                        .content(
                            "① 쌍무계약의 당사자 일방은 상대방이 그 채무이행을 제공할 때까지 자기의 채무이행을 거절할 수 있다. 그러나 상대방의 채무가 변제기에 있지 아니하는 때에는 그러하지 아니하다.")
                        .build(),
                    com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteChunk.builder()
                        .docId("statute-4")
                        .title("민법 제623조 (임대인의 의무)")
                        .content("임대인은 목적물을 임차인에게 인도하고 계약존속 중 그 사용·수익에 필요한 상태를 유지하게 할 의무를 부담한다.")
                        .build(),
                    com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteChunk.builder()
                        .docId("statute-5")
                        .title("민법 제626조 (임차인의 상환청구권)")
                        .content(
                            "① 임차인이 임차물의 보존에 관한 필요비를 지출한 때에는 임대인에 대하여 그 상환을 청구할 수 있다. ② 임차인이 유익비를 지출한 경우에는 임대인은 임대차 종료 시에 그 가액의 증가가 현존한 때에 한하여 임차인의 지출한 금액이나 그 증가액을 상환하여야 한다.")
                        .build(),
                    com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteChunk.builder()
                        .docId("statute-6")
                        .title("민법 제640조 (차임연체와 해지)")
                        .content("건물 기타 공작물의 임대차에는 임차인의 차임연체액이 2기의 차임액에 달하는 때에는 임대인은 계약을 해지할 수 있다.")
                        .build(),
                    com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteChunk.builder()
                        .docId("statute-7")
                        .title("주택임대차보호법 제3조의3 (보증금의 회수)")
                        .content("① 임차인은 임차주택을 양수인에게 인도하지 아니하면 보증금을 받을 수 없다. ② 임차인은 보증금을 반환받을 때까지 임차주택을 인도하지 아니할 수 있다.")
                        .build()))
                .build()))
        .build();

    // ResearchedMessage 생성
    ResearchedMessage researchedMessage = ResearchedMessage.builder()
        .role("user")
        .content(lastQuery)
        .researchResult(sampleResult)
        .build();

    // LLM 요청 생성
    List<IMessageable> messages = List.of(systemMessage, researchedMessage);

    AdvancedCompletionRequest completionRequest = AdvancedCompletionRequest.builder()
        // .model("gpt-4.1")
        // .model("gemini-2.5-flash")
        // .model("luxia3-llm-32b-0901-Q")
        .model("luxia3-deep-32b-0901-Q")
        .messages(messages)
        .build();

    DirectLlmRequest request = DirectLlmRequest.builder()
        // .baseUrl("https://api.openai.com")
        // .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
        // .baseUrl("http://172.16.100.200:14100")
        .baseUrl("http://172.16.100.200:14101")
        // .apiKey("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")
        // .apiKey("AIzaSyBrDhx_moR0WgO3nBiCEf4fhrk6gipczBk")
        .body(completionRequest)
        .build();

    // when - streamLlm 호출
    StringBuilder contentBuilder = new StringBuilder();
    List<CitedMessage> receivedDeltas = new ArrayList<>();
    CountDownLatch llmLatch = new CountDownLatch(1);
    AtomicReference<Throwable> errorRef = new AtomicReference<>();

    WorkflowContext<AdvancedCompletionResponse> llmContext = client.streamLlm(
        request,
        new WorkflowListener<>() {
          @Override
          public void onNext(AdvancedCompletionResponse item) {
            try {
              System.out.println(JSON.writeValueAsString(item));
            } catch (Exception e) {
              System.out.println("[JSON ERROR] " + e.getMessage());
            }
            if (item.getChoices() != null && !item.getChoices().isEmpty()) {
              CitedMessage delta = item.getChoices().get(0).getDelta();
              if (delta != null) {
                receivedDeltas.add(delta);
                if (delta.getContent() != null) {
                  contentBuilder.append(delta.getContent());
                }
              }
            }
          }

          @Override
          public void onError(Throwable e) {
            System.err.println("\n[ERROR] " + e.getMessage());
            e.printStackTrace();
            errorRef.set(e);
            llmLatch.countDown();
          }

          @Override
          public void onComplete() {
            System.out.println("\n[COMPLETE]");
            llmLatch.countDown();
          }

          @Override
          public void onCancel() {
            System.out.println("\n[CANCEL]");
            llmLatch.countDown();
          }
        });

    // then - 5분 타임아웃
    boolean llmCompleted = llmLatch.await(5, TimeUnit.MINUTES);

    assertThat(llmCompleted).as("LLM 스트리밍이 완료되어야 함").isTrue();
    assertThat(errorRef.get()).as("에러가 없어야 함").isNull();
    assertThat(receivedDeltas).as("델타가 수신되어야 함").isNotEmpty();

    String finalContent = contentBuilder.toString();
    assertThat(finalContent).as("응답 내용이 있어야 함").isNotBlank();

    System.out.println("\n=== streamLlm 최종 결과 ===");
    try {
      System.out.println(JSON.writeValueAsString(llmContext.get()));
    } catch (Exception e) {
      System.out.println("[JSON ERROR] " + e.getMessage());
    }
  }

  @Test
  @DisplayName("완전 통합 테스트 - deepresearch -> systemPrompt -> streamLlm")
  void fullIntegration_deepresearchToStreamLlm() throws Exception {
    // === 1단계: DeepResearch로 법률 검색 ===
    System.out.println("=== 1단계: DeepResearch 실행 ===");

    List<Message> history = List.of();
    String lastQuery = "임대차 계약 해지 시 보증금 반환 기한은 어떻게 되나요?";

    CountDownLatch deepresearchLatch = new CountDownLatch(1);

    WorkflowContext<ResearchResult> deepresearchContext = client.deepresearch(
        "luxia3-deep-32b-0901-Q",
        history,
        lastQuery,
        new WorkflowListener<>() {
          @Override
          public void onNext(ResearchResult item) {
            try {
              System.out.println(JSON.writeValueAsString(item));
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          @Override
          public void onComplete() {
            deepresearchLatch.countDown();
          }

          @Override
          public void onCancel() {
            deepresearchLatch.countDown();
          }
        });

    deepresearchLatch.await(5, TimeUnit.MINUTES);
    ResearchResult researchResult = deepresearchContext.get();

    System.out.println("\n=== DeepResearch 최종 결과 ===");
    System.out.println(JSON.writeValueAsString(researchResult));

    // === 2단계: 시스템 프롬프트 조회 ===
    System.out.println("\n=== 2단계: 시스템 프롬프트 조회 ===");

    String systemPromptContent = client.systemPrompt("deepresearch");
    System.out.println(systemPromptContent);

    // === 3단계: streamLlm으로 최종 응답 생성 ===
    System.out.println("\n=== 3단계: streamLlm 실행 ===");

    ResearchedMessage systemMessage = ResearchedMessage.builder()
        .role("system")
        .content(systemPromptContent)
        .build();

    ResearchedMessage userMessage = ResearchedMessage.builder()
        .role("user")
        .content(lastQuery)
        .researchResult(researchResult)
        .build();

    AdvancedCompletionRequest completionRequest = AdvancedCompletionRequest.builder()
        .model("luxia3-deep-32b-0901-Q")
        .messages(List.of(systemMessage, userMessage))
        .build();

    DirectLlmRequest llmRequest = DirectLlmRequest.builder()
        .baseUrl("http://172.16.100.200:14101")
        .body(completionRequest)
        .build();

    CountDownLatch llmLatch = new CountDownLatch(1);

    WorkflowContext<AdvancedCompletionResponse> llmContext = client.streamLlm(
        llmRequest,
        new WorkflowListener<>() {
          @Override
          public void onNext(AdvancedCompletionResponse item) {
            try {
              System.out.println(JSON.writeValueAsString(item));
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          @Override
          public void onComplete() {
            llmLatch.countDown();
          }

          @Override
          public void onCancel() {
            llmLatch.countDown();
          }
        });

    llmLatch.await(5, TimeUnit.MINUTES);

    System.out.println("\n=== streamLlm 최종 결과 ===");
    System.out.println(JSON.writeValueAsString(llmContext.get()));
  }
}
