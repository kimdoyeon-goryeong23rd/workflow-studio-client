# CONTRIBUTING

이 문서는 workflow-studio-client 라이브러리를 확장 개발할 때 따라야 할 규칙을 정의합니다.

## 일반 개발 규칙

### 1. 패키지로 분리

구현하고자 하는 플로우들 및 플로우를 사용한 로직을 독립된 패키지로 관리하세요.

### 2. Payload 정의

각 패키지의 `payload/` 디렉토리에 플로우별 요청/응답 DTO를 정의합니다.

```java
// payload/YourFlowPayloads.java
public class YourFlowPayloads {

    @Builder @Getter
    public static class YourRequest {
        private String query;
        private List<Message> history;
    }

    @Builder @Getter
    public static class YourResponse {
        private String result;
    }
}
```

**규칙:**

- `@Builder`, `@Getter` 사용

### 3. 타입 참조 상수

Java의 **제네릭 소거(Type Erasure)** 때문에 `ParameterizedTypeReference`를 사용합니다.

```java
// 타입 참조 상수는 클래스 상단에 private static final로 선언
private static final ParameterizedTypeReference<
    WorkflowResponse<YourResponse>
> YOUR_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};
```

**상수로 선언하는 이유:**

- 매번 익명 클래스를 생성하면 불필요한 객체 생성
- 동일한 타입 참조는 재사용 가능
- 클래스 상단에 모아두면 지원하는 응답 타입을 한눈에 파악 가능

## direct 패키지 확장

`direct/` 패키지는 워크플로우 서버를 우회하여 외부 API를 직접 호출해야 하는 경우 확장 개발할 수 있습니다.

### DirectLlmProcessor 사용

```java
DirectLlmProcessor llmClient = new DirectLlmProcessor(webClientBuilder, objectMapper);

// 스트리밍 호출
llmClient.stream(request).subscribe(response -> { ... });

// Context 기반 스트리밍
llmClient.streamToContext(request, context);

// NEW! ReasoningObject로 파싱하며 스트리밍
llmClient.streamToReasoningObject(request, context, typeReference);
```

### 새 직접 호출 클라이언트 추가

1. `direct/` 디렉토리에 `Direct*Processor.java` 생성
2. `direct/payload/`에 요청/응답 DTO 정의

## WorkflowClient 재정의

`WorkflowClient`에 새로 만든 processor들을 동봉하여 공개할 메서드들을 연결합니다.

### 구조 예시

```
mydomain/
├── MyDomainProcessor.java            # 도메인 비즈니스 로직
├── common/
│   └── MyDomainProperties.java       # 설정
└── payload/                          # 요청/응답 DTO
```

### 확장 클라이언트 작성

```java
public class WorkflowClient extends BaseWorkflowClient {
    private final MyDomainProcessor domainProcessor;

    public WorkflowClient(
        WebClient.Builder builder,
        String url,
        String apiKey,
        MyDomainProperties properties
    ) {
        super(builder, url, apiKey);
        this.domainProcessor = new MyDomainProcessor(processor, properties, llmClient);
    }

    public MyResponse myDomainFlow(MyRequest request) {
        return domainProcessor.execute(request);
    }
}
```

### Processor 작성

```java
public class MyDomainProcessor {

    // 1. 타입 참조 상수 정의
    private static final ParameterizedTypeReference<
        WorkflowResponse<YourResponse>
    > YOUR_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private final WorkflowProcessor processor;
    private final MyDomainProperties properties;
    private final DirectLlmProcessor llmClient;

    // 2. 내부 플로우 메서드 (package-private for testing)
    YourResponse yourFlow(final YourRequest req) {
        return processor.getResult(
            properties.getYourFlowPath(),
            req,
            YOUR_RESPONSE_TYPE
        );
    }

    // 3. 공개 워크플로우 메서드
    public WorkflowContext<YourResult> yourWorkflow(
        final YourInput input,
        final WorkflowListener<YourResult> listener
    ) {
        final WorkflowContext<YourResult> context = new WorkflowContext<>(listener);

        CompletableFuture.runAsync(() -> {
            try {
                context.checkCancelled();
                var result = yourFlow(new YourRequest(input));
                context.emitNext(YourResult.builder().data(result).build());
                context.emitComplete();
            } catch (CancellationException e) {
                // 정상적인 취소
            } catch (Exception e) {
                context.emitError(e);
            }
        });

        return context;
    }
}
```

**규칙:**

- 비즈니스 로직은 Processor 클래스에 분리
- 내부 플로우 메서드는 package-private (테스트 가능)
- 스트림 출력 메서드는 context-listener 패턴 사용
- `context.checkCancelled()` 호출로 취소 지점 설정
- 비동기 실행은 `CompletableFuture.runAsync()` 사용

## 메서드 설계 원칙

| 기준               | 선택                                              |
| ------------------ | ------------------------------------------------- |
| 스트림 출력 필요   | `WorkflowContext<T>` + `WorkflowListener<T>` 반환 |
| 단건 결과만 필요   | DTO 직접 반환                                     |
| 워크플로우 호출    | `BaseWorkflowClient` 또는 확장 클라이언트 사용    |
| 외부 API 직접 호출 | `direct/` 패키지의 Processor 사용                 |

## 체크리스트

### core 패키지 수정 시:

- [ ] 하위 호환성 확인
- [ ] 모든 확장 클라이언트에 영향 없는지 확인
- [ ] Javadoc 작성

### 새 직접 호출 클라이언트 추가 시:

- [ ] `direct/`에 Processor 클래스 생성
- [ ] `direct/payload/`에 DTO 정의
- [ ] `direct/common/`에 예외 클래스 정의
- [ ] Javadoc 작성

### 새 도메인 클라이언트 추가 시 (별도 브랜치):

- [ ] `mydomain/` 패키지 생성
- [ ] `MyDomainProcessor.java` 작성
- [ ] `mydomain/common/`에 Properties 클래스 정의
- [ ] `mydomain/payload/`에 Request/Response DTO 정의
- [ ] `BaseWorkflowClient`를 상속한 클라이언트 작성
- [ ] Javadoc 작성
