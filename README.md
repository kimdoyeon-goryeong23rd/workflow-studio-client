# Workflow Studio Client

Workflow Studio와 통신하는 Java 클라이언트 SDK입니다.

SpringBoot 프로젝트에서 Bean으로 등록하여 사용할 수 있습니다.

## 설치

### Gradle

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.kimdoyeon-goryeong23rd:workflow-studio-client:0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.kimdoyeon-goryeong23rd</groupId>
    <artifactId>workflow-studio-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

## 빠른 시작

```java
WebClient.Builder clientBuilder = WebClient.builder();
FlowPathProperties properties = FlowPathProperties.builder().build();

WorkflowClient client = new WorkflowClient(
    clientBuilder,
    "http://your-workflow-server/luxia-workflow-processor",
    "your-api-key",
    properties
);
```

---

## API 요약

### 단순 메서드 (동기)

| 메서드 | 입력 | 출력 | 설명 |
|--------|------|------|------|
| `models()` | - | `ModelInfoResponse` | 사용 가능한 LLM 모델 목록 조회 |
| `systemPrompt(type)` | `String` | `String` | 시스템 프롬프트 조회 (예: "deepresearch") |
| `titleGeneration(req)` | `TitleGenerationRequest` | `TitleGenerationResponse` | 대화 내용 기반 채팅방 제목 생성 |
| `intentClassification(req)` | `ChatRequest` | `IntentClassificationResponse` | 사용자 질의 의도 분류 |

### 스트림 메서드 (비동기)

| 메서드 | 입력 | 출력 | 설명 |
|--------|------|------|------|
| `deepresearch(...)` | model, history, lastQuery, listener | `WorkflowContext<ResearchResult>` | 법령/판례 검색 및 분석 |
| `streamLlm(...)` | DirectLlmRequest, listener | `WorkflowContext<AdvancedCompletionResponse>` | LLM 스트리밍 응답 생성 |

---

## 스트림 메서드 활용

스트림 메서드는 **OpenAI Chat Completion 스타일의 델타 스트림**을 제공합니다.

### 핵심 개념: WorkflowContext

```java
WorkflowContext<T> context = client.streamMethod(..., listener);

// 스트림 이벤트는 listener.onNext()로 실시간 수신
// 완료 후 최종 결과는 context.get()으로 동기 대기
T finalResult = context.get();
```

### 백엔드 활용 패턴

**핵심 포인트:**
- `onNext()`: 델타 청크를 **바로 프론트엔드로 전달** (기록 불필요)
- `get()`: 스트림 완료 후 **완성된 결과를 DB에 저장**

```java
// 프론트엔드로 스트림 전달 + 최종 결과 저장 패턴
CountDownLatch latch = new CountDownLatch(1);

WorkflowContext<ResearchResult> ctx = client.deepresearch(
    model, history, query,
    new WorkflowListener<>() {
        @Override
        public void onNext(ResearchResult item) {
            // 델타를 프론트엔드로 바로 전송 (저장 불필요)
            sseEmitter.send(objectMapper.writeValueAsString(item));
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }
    }
);

latch.await();
ResearchResult finalResult = ctx.get();  // 완성된 결과
repository.save(finalResult);            // DB 저장
```

---

## deepresearch

법률 관련 질의에 대해 법령과 판례를 검색하고 분석하는 심층 연구 워크플로우입니다.

### 사용법

```java
CountDownLatch latch = new CountDownLatch(1);

WorkflowContext<ResearchResult> context = client.deepresearch(
    "luxia3-deep-32b-0901-Q",           // 모델명
    List.of(),                           // 이전 대화 이력
    "임대차 계약 해지 시 보증금 반환 기한은?",  // 사용자 질의
    new WorkflowListener<>() {
        @Override
        public void onNext(ResearchResult item) {
            // 각 단계 완료 시 partial 결과 수신
            System.out.println(objectMapper.writeValueAsString(item));
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }
    }
);

latch.await();
ResearchResult result = context.get();  // 최종 결과
```

### ResearchResult 객체

```java
public class ResearchResult {
    SelfQueryResponse selfQuery;           // 쿼리 분석 결과 (필터, 시맨틱 쿼리)
    String searchQuery;                    // 검색에 사용된 쿼리
    List<IRetrievalFlow> retrievalFlows;   // 검색 시도 결과들
    String reason;                         // 전체 분석 이유
    Sufficiency sufficiency;               // 자료 충분성 (pass/fail)
    String plan;                           // 답변 작성 계획
    Boolean error;                         // 에러 발생 여부
}
```

**retrievalFlows (검색 결과):**

각 검색 시도가 `IRetrievalFlow`로 기록됩니다.

| 필드 | 설명 |
|------|------|
| `type` | "statute" (법령) 또는 "precedent" (판례) |
| `index` | 배열 내 순서 |
| `expandedQueries` | 확장된 검색 쿼리들 |
| `documentCount` | 검색된 문서 수 |
| `reason` | 해당 검색 결과에 대한 분석 |
| `sufficiency` | 해당 검색의 충분성 판단 (pass/fail) |

**문서 접근:**
```java
// 모든 검색 결과에서 중복 제거된 문서 목록
List<IDocument> allDocs = result.getAllDocuments();

for (IDocument doc : allDocs) {
    doc.getId();      // 문서 ID
    doc.getTitle();   // 문서 제목
    doc.getUrl();     // 상세 페이지 URL (법제처/대법원 링크)
}
```

### onNext 델타 예시

각 단계가 완료될 때마다 해당 필드만 채워진 partial 객체가 emit됩니다.

```json
// 1. selfQuery (쿼리 분석)
{"selfQuery":{"statuteFilter":{"title":"주택임대차보호법"},"precedentFilter":{"caseType":"민사"},"semanticQuery":"사용자는 임대차 계약 해지 시 보증금 반환 기한에 관한 정보를 알고자 합니다...","baseDate":20251228}}

// 2. searchQuery (재구성된 검색 쿼리)
{"searchQuery":"임대차 계약 해지 시 보증금 반환 기한에 대해 알고 싶습니다."}

// 3. retrievalFlows - 쿼리 확장 (법령)
{"retrievalFlows":[{"type":"statute","index":0,"expandedQueries":["임대차 계약 종료 후 보증금 반환 시기에 대해 알려주세요.","집 보증금 반환 기한은 계약 해지 후 언제까지인가요?","임대차 계약 해지 시 보증금을 언제까지 돌려받아야 하나요?"]}]}

// 4. retrievalFlows - 쿼리 확장 (판례)
{"retrievalFlows":[{"type":"precedent","index":1,"expandedQueries":["임대차 계약 종료 후 보증금 반환 시기에 대해 알려주세요.","집 보증금 반환 기한은 계약 해지 후 언제까지인가요?"]}]}

// 5. retrievalFlows - 검색 결과 (문서 수)
{"retrievalFlows":[{"type":"statute","index":0,"documentCount":3}]}

// 6. retrievalFlows - 분석 reason (스트리밍, 토큰 단위)
{"retrievalFlows":[{"type":"statute","index":0,"reason":"임"}]}
{"retrievalFlows":[{"type":"statute","index":0,"reason":"대"}]}
{"retrievalFlows":[{"type":"statute","index":0,"reason":"차"}]}
// ... (reason이 토큰 단위로 스트리밍됨)

// 7. retrievalFlows - 충분성 판단
{"retrievalFlows":[{"type":"statute","index":0,"sufficiency":"fail"}]}
```

---

## streamLlm

OpenAI 호환 LLM API에 직접 스트리밍 요청을 보냅니다. 인용(Citation) 처리가 포함됩니다.

### 사용법

```java
// 메시지 구성 (ResearchResult 포함 가능)
ResearchedMessage systemMsg = ResearchedMessage.builder()
    .role("system")
    .content(client.systemPrompt("deepresearch"))
    .build();

ResearchedMessage userMsg = ResearchedMessage.builder()
    .role("user")
    .content("임대차 계약 해지 시 보증금 반환 기한은?")
    .researchResult(researchResult)  // deepresearch 결과 첨부
    .build();

// 요청 생성
AdvancedCompletionRequest completionReq = AdvancedCompletionRequest.builder()
    .model("luxia3-deep-32b-0901-Q")
    .messages(List.of(systemMsg, userMsg))
    .build();

DirectLlmRequest request = DirectLlmRequest.builder()
    .baseUrl("http://llm-server:port")
    .body(completionReq)
    .build();

// 스트리밍 호출
CountDownLatch latch = new CountDownLatch(1);

WorkflowContext<AdvancedCompletionResponse> context = client.streamLlm(
    request,
    new WorkflowListener<>() {
        @Override
        public void onNext(AdvancedCompletionResponse chunk) {
            // Chat Completion 스타일 델타 청크
            System.out.println(objectMapper.writeValueAsString(chunk));
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }
    }
);

latch.await();
AdvancedCompletionResponse result = context.get();
```

### AdvancedCompletionResponse 객체

OpenAI Chat Completion API와 동일한 구조입니다.

```java
public class AdvancedCompletionResponse {
    String id;                              // 응답 ID (예: "chatcmpl-xxx")
    String object;                          // "chat.completion.chunk"
    Long created;                           // 생성 시간 (Unix timestamp)
    String model;                           // 사용된 모델
    List<Choice<CitedMessage>> choices;     // 응답 선택지
    Usage usage;                            // 토큰 사용량
}
```

**Choice 구조:**

| 필드 | 설명 |
|------|------|
| `delta` | 스트리밍 시 부분 메시지 (role, content, reasoning) |
| `message` | 완료 시 전체 메시지 |
| `finishReason` | 종료 사유 ("stop" 등) |

**CitedMessage (인용 포함 메시지):**

```java
public class CitedMessage {
    String role;              // "assistant"
    String content;           // 응답 내용
    String reasoning;         // 추론 과정 (선택적)
    List<Citation> citations; // 인용 정보

    // 인용 태그가 포함된 content 반환
    String getContentWithCitations();
    // 예: "내용<cite><id>doc1</id>인용부분</cite>내용"
}
```

**Citation 구조:**

| 필드 | 설명 |
|------|------|
| `id` | 인용된 문서 ID (ResearchResult의 IDocument.getId()와 매칭) |
| `startIndex` | content 내 시작 위치 |
| `endIndex` | content 내 끝 위치 |

### onNext 델타 예시

OpenAI Chat Completion 스타일의 스트리밍 청크입니다.

```json
// reasoning 스트리밍 (토큰 단위)
{"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1766878183,"model":"luxia3-deep-32b-0901-Q","choices":[{"index":0,"delta":{"reasoning":"임"}}]}
{"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1766878183,"model":"luxia3-deep-32b-0901-Q","choices":[{"index":0,"delta":{"reasoning":"대"}}]}
{"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1766878183,"model":"luxia3-deep-32b-0901-Q","choices":[{"index":0,"delta":{"reasoning":"차"}}]}
// ... (reasoning이 먼저 스트리밍됨)

// content 스트리밍 (reasoning 완료 후)
{"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1766878183,"model":"luxia3-deep-32b-0901-Q","choices":[{"index":0,"delta":{"content":"### 1."}}]}
// ...
```

### get() 최종 결과 예시

스트림 완료 후 `context.get()`으로 받는 완성된 응답입니다.

```json
{
  "id": "chatcmpl-8cbb49fe91063715",
  "object": "chat.completion.chunk",
  "created": 1766878183,
  "model": "luxia3-deep-32b-0901-Q",
  "choices": [{
    "index": 0,
    "delta": {
      "role": "assistant",
      "content": "### 1. 임대차계약 해지 시 보증금 반환의무 발생 조건\n임대차계약이 해지될 때 임대인이 임차인에게 보증금을 반환해야 하는 의무가 발생한다는 점은 「주택임대차보호법」 제3조(신용보증)와 대법원 판례(2024다302217)에서 확인할 수 있습니다. ◖1◗ ◖3◗\n\n### 2. 보증금 반환 절차와 우선변제권\n...",
      "reasoning": "임대차계약 해지 시 보증금 반환 기한에 관한 최종 답변을 작성하기 위해서는 먼저 제공된 자료들에서 관련 법령과 판례가 이 사안을 직접적으로 규정하고 있는지를 면밀히 검토해야 합니다..."
    },
    "finish_reason": "stop"
  }]
}
```

> **참고**: `◖1◗` 형식은 인용 마커로, `citations` 배열과 함께 `getContentWithCitations()` 메서드로 `<cite>` 태그 형식으로 변환할 수 있습니다.

---

## 전체 통합 예시

DeepResearch → SystemPrompt → StreamLlm 전체 흐름:

```java
// === 1단계: DeepResearch로 법률 자료 검색 ===
CountDownLatch latch1 = new CountDownLatch(1);

WorkflowContext<ResearchResult> researchCtx = client.deepresearch(
    "luxia3-deep-32b-0901-Q",
    List.of(),
    "임대차 계약 해지 시 보증금 반환 기한은?",
    new WorkflowListener<>() {
        @Override
        public void onNext(ResearchResult item) {
            sendToFrontend(objectMapper.writeValueAsString(item));
        }

        @Override
        public void onComplete() { latch1.countDown(); }
    }
);

latch1.await();
ResearchResult research = researchCtx.get();

// === 2단계: 시스템 프롬프트 조회 (동기) ===
String systemPrompt = client.systemPrompt("deepresearch");

// === 3단계: StreamLlm으로 최종 답변 생성 ===
ResearchedMessage sysMsg = ResearchedMessage.builder()
    .role("system")
    .content(systemPrompt)
    .build();

ResearchedMessage userMsg = ResearchedMessage.builder()
    .role("user")
    .content("임대차 계약 해지 시 보증금 반환 기한은?")
    .researchResult(research)
    .build();

DirectLlmRequest llmReq = DirectLlmRequest.builder()
    .baseUrl("http://llm-server:port")
    .body(AdvancedCompletionRequest.builder()
        .model("luxia3-deep-32b-0901-Q")
        .messages(List.of(sysMsg, userMsg))
        .build())
    .build();

CountDownLatch latch2 = new CountDownLatch(1);

WorkflowContext<AdvancedCompletionResponse> llmCtx = client.streamLlm(
    llmReq,
    new WorkflowListener<>() {
        @Override
        public void onNext(AdvancedCompletionResponse chunk) {
            sendToFrontend(objectMapper.writeValueAsString(chunk));
        }

        @Override
        public void onComplete() { latch2.countDown(); }
    }
);

latch2.await();
AdvancedCompletionResponse answer = llmCtx.get();

// === 최종 결과 저장 ===
saveToDatabase(research, answer);
```

---

## 단순 메서드 상세

### models()

사용 가능한 LLM 모델 목록을 조회합니다.

```java
ModelInfoResponse response = client.models();

for (ModelInfo model : response.getModels()) {
    model.getName();     // 모델 전체명
    model.getAbbr();     // 약어
    model.getBaseUrl();  // API 엔드포인트
}
```

### systemPrompt(type)

시스템 프롬프트를 조회합니다.

```java
String prompt = client.systemPrompt("deepresearch");
```

### titleGeneration(req)

대화 내용을 기반으로 채팅방 제목을 생성합니다.

```java
TitleGenerationRequest req = TitleGenerationRequest.builder()
    .query("임대차 계약 해지 시 보증금 반환 기한은?")
    .answer("임대차계약이 해지되면 임대인은...")
    .build();

TitleGenerationResponse res = client.titleGeneration(req);
res.getTitle();  // "임대차 보증금 반환 기한"
```

### intentClassification(req)

사용자 질의의 의도를 분류합니다.

```java
ChatRequest req = ChatRequest.builder()
    .history(previousMessages)
    .lastQuery("요약해줘")
    .build();

IntentClassificationResponse res = client.intentClassification(req);

res.isSummary();     // 요약 요청
res.isSearch();      // 검색 필요
res.isSmalltalk();   // 일상 대화
res.isTransition();  // 주제 전환
```

---

## WorkflowListener 인터페이스

```java
public interface WorkflowListener<T> {
    void onNext(T item);                  // 스트림 이벤트 수신
    void onComplete();                    // 정상 완료
    default void onError(Throwable e) {} // 에러 발생
    default void onCancel() {}           // 취소됨
}
```

---

## WorkflowContext 메서드

| 메서드 | 설명 |
|--------|------|
| `get()` | 완료 대기 후 결과 반환 (블로킹) |
| `toFuture()` | `CompletableFuture<T>` 반환 |
| `cancel()` | 워크플로우 취소 |
| `isCancelled()` | 취소 여부 확인 |

---

## 패키지 구조

```
workflow/
├── core/                         # 기본 클라이언트 및 스키마
│   ├── BaseWorkflowClient.java   # 기본 클라이언트 (상속하여 확장 가능)
│   ├── common/                   # 핵심 클래스 (Processor, Context, Listener 등)
│   └── payload/                  # 공통 DTO (WorkflowResponse)
│
├── direct/                       # 워크플로우 우회 직접 호출 패키지
│   ├── DirectLlmProcessor.java   # OpenAI 호환 LLM 스트리밍 클라이언트
│   ├── common/                   # 핵심 클래스 (MessageAssembler 등)
│   └── payload/                  # 요청/응답 DTO
│
├── deepresearch/                 # 심층 연구 워크플로우 패키지
│   ├── DeepresearchProcessor.java # 심층 연구 워크플로우 서비스
│   ├── common/                   # 설정 클래스 (FlowPathProperties)
│   └── payload/                  # 요청/응답 DTO
│
└── WorkflowClient.java           # BaseWorkflowClient 확장 구현체
```

---

## Spring에서 사용하기

```java
@Configuration
public class WorkflowConfig {

    @Bean
    public WorkflowClient workflowClient(
        WebClient.Builder builder,
        @Value("${workflow.url}") String url,
        @Value("${workflow.api-key}") String apiKey
    ) {
        FlowPathProperties properties = FlowPathProperties.builder().build();
        return new WorkflowClient(builder, url, apiKey, properties);
    }
}
```

---

## 라이선스

Saltlux Proprietary License - 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.
