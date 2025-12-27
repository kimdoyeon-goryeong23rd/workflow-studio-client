package com.saltlux.workflow.direct.common;

import java.util.ArrayList;
import java.util.List;

import com.saltlux.workflow.direct.payload.messageable.Citation;
import com.saltlux.workflow.direct.payload.messageable.CitedMessage;
import com.saltlux.workflow.direct.payload.messageable.ResponseMessage;

import lombok.extern.slf4j.Slf4j;
import me.hanju.adapter.ContentStreamAdapter;
import me.hanju.adapter.payload.TaggedToken;
import me.hanju.adapter.transition.TransitionSchema;

/**
 * LLM 스트리밍 응답에서 cite/rag 태그를 파싱하여 Citation을 추출하고,
 * {@link CitedMessage}를 생성하는 어셈블러.
 *
 * <p>
 * 스트리밍 중:
 * <ul>
 * <li>{@link #processToken}으로 각 토큰 처리 (cite 태그 파싱)</li>
 * <li>{@link #appendReasoning}으로 reasoning 누적</li>
 * <li>{@link #mergeToolCalls}로 toolCalls 누적</li>
 * </ul>
 * 스트림 종료 시:
 * <ul>
 * <li>{@link #flush}로 버퍼 정리</li>
 * <li>{@link #buildFinalMessage}로 최종 메시지 생성</li>
 * </ul>
 * </p>
 */
@Slf4j
public class CitedMessageAssembler extends MessageAssembler<CitedMessage> {

  /** cite/rag 태그와 그 안의 id 태그를 인식하는 스키마 */
  private static final TransitionSchema CITE_SCHEMA = TransitionSchema.root()
      .tag("cite", cite -> cite.tag("id")).alias("rag");

  // cite/rag 태그 파싱용 어댑터
  private final ContentStreamAdapter adapter = new ContentStreamAdapter(CITE_SCHEMA);

  // 현재 진행 중인 cite의 id 누적
  private final StringBuilder citeIdBuilder = new StringBuilder();

  // 완성된 Citation 리스트
  private final List<Citation> citations = new ArrayList<>();

  // 현재 텍스트 위치 (Citation의 startIndex/endIndex 계산용)
  private int currentIndex = 0;

  // 다음 Citation의 index
  private int citationIndex = 0;

  // 현재 진행 중인 cite의 시작 위치 (null이면 cite 태그 밖)
  private Integer citeStartIndex = null;

  /**
   * delta를 처리하여 누적하고, 스트리밍용 CitedMessage 리스트를 반환한다.
   *
   * <p>
   * delta의 각 필드를 처리:
   * <ul>
   * <li>reasoning: 누적 후 그대로 메시지에 포함</li>
   * <li>toolCalls: index 기반 병합 후 그대로 메시지에 포함</li>
   * <li>content: cite 태그 파싱하여 가공된 텍스트와 Citation 분리</li>
   * </ul>
   * </p>
   *
   * @param delta 처리할 delta (nullable)
   * @return 스트리밍용 CitedMessage 리스트 (content가 없거나 cite 태그만 있으면 빈 리스트)
   */
  public List<CitedMessage> processDelta(final ResponseMessage delta) {
    if (delta == null) {
      return List.of();
    }

    // reasoning, toolCalls 누적
    appendReasoning(delta.getReasoning());
    mergeToolCalls(delta.getToolCalls());

    // content가 없으면 reasoning/toolCalls만 있는 메시지 반환
    if (delta.getContent() == null || delta.getContent().isEmpty()) {
      // reasoning이나 toolCalls가 있으면 메시지 생성 (빈 문자열 제외)
      final boolean hasReasoning = delta.getReasoning() != null && !delta.getReasoning().isEmpty();
      final boolean hasToolCalls = delta.getToolCalls() != null && !delta.getToolCalls().isEmpty();
      if (hasReasoning || hasToolCalls) {
        return List.of(CitedMessage.builder()
            .role(delta.getRole())
            .reasoning(hasReasoning ? delta.getReasoning() : null)
            .toolCalls(hasToolCalls ? delta.getToolCalls() : null)
            .build());
      }
      return List.of();
    }

    // content 처리: cite 태그 파싱
    final List<CitedMessage> messages = new ArrayList<>();
    for (final TokenProcessResult result : processToken(delta.getContent())) {
      messages.add(CitedMessage.builder()
          .role(delta.getRole())
          .content(result.content())
          .reasoning(delta.getReasoning())
          .toolCalls(delta.getToolCalls())
          .citations(result.citation() != null ? List.of(result.citation()) : null)
          .build());
    }
    return messages;
  }

  /**
   * content 토큰을 처리하여 텍스트를 누적하고, 처리 결과를 반환한다.
   *
   * <p>
   * ContentStreamAdapter를 통해 cite/rag 태그를 파싱하여:
   * <ul>
   * <li>일반 텍스트: textBuilder에 누적, content 반환</li>
   * <li>cite 태그 열림: startIndex 기록</li>
   * <li>cite 태그 닫힘: Citation 생성 및 반환</li>
   * <li>cite 내부 텍스트: textBuilder에 누적, content 반환</li>
   * <li>id 태그 내용: citeIdBuilder에 누적 (content 반환 안함)</li>
   * </ul>
   * </p>
   *
   * @param content 처리할 content 토큰
   * @return 처리 결과 리스트 (각 토큰별 content와 Citation)
   */
  private List<TokenProcessResult> processToken(final String content) {
    if (content == null || content.isEmpty()) {
      return List.of();
    }

    final List<TokenProcessResult> results = new ArrayList<>();
    for (final TaggedToken token : adapter.feedToken(content)) {
      final TokenProcessResult result = processTaggedToken(token);
      if (result.citation() != null) {
        citations.add(result.citation());
      }
      if (result.content() != null || result.citation() != null) {
        results.add(result);
      }
    }
    return results;
  }

  /**
   * 스트림 종료 시 버퍼에 남은 데이터를 flush한다.
   *
   * <p>
   * 다음 순서로 처리:
   * <ol>
   * <li>adapter 버퍼에 남은 토큰들 처리</li>
   * <li>닫히지 않은 cite 태그 처리</li>
   * </ol>
   * </p>
   */
  public void flush() {
    // adapter 버퍼에 남은 토큰들 처리
    for (final TaggedToken token : adapter.flush()) {
      final TokenProcessResult result = processTaggedToken(token);
      if (result.citation() != null) {
        citations.add(result.citation());
      }
    }

    // 닫히지 않은 cite 태그 처리: 스트림이 끝났는데 </cite>가 안 온 경우
    if (citeStartIndex != null) {
      citations.add(Citation.builder()
          .index(citationIndex++)
          .id(citeIdBuilder.toString())
          .startIndex(citeStartIndex)
          .endIndex(currentIndex)
          .build());
      citeStartIndex = null;
    }
  }

  @Override
  public CitedMessage buildFinalMessage() {
    return CitedMessage.builder()
        .role("assistant")
        .content(getContent())
        .reasoning(getReasoning())
        .toolCalls(getToolCalls())
        .citations(List.copyOf(citations))
        .build();
  }

  /**
   * TaggedToken을 처리하여 텍스트를 누적하고, 처리 결과를 반환한다.
   */
  private TokenProcessResult processTaggedToken(final TaggedToken token) {
    final String path = token.path();
    final String content = token.content();
    final String event = token.event();

    log.info("[TOKEN] path={}, event={}, content={}", path, event, content);

    if ("/".equals(path)) {
      // 루트 경로: 일반 텍스트 (cite 태그 바깥)
      if (content != null) {
        textBuilder.append(content);
        currentIndex += content.length();
        return new TokenProcessResult(content, null);
      }
    } else if ("/cite".equals(path)) {
      // cite 태그 경로
      if ("OPEN".equals(event)) {
        // <cite> 태그 시작: 현재 위치를 startIndex로 기록
        citeStartIndex = currentIndex;
        citeIdBuilder.setLength(0);
        log.info("[CITE OPEN] startIndex={}", citeStartIndex);
      } else if ("CLOSE".equals(event)) {
        // </cite> 태그 종료: Citation 생성 및 반환
        log.info("[CITE CLOSE] startIndex={}, endIndex={}", citeStartIndex, currentIndex);
        final Citation citation = Citation.builder()
            .index(citationIndex++)
            .id(citeIdBuilder.toString())
            .startIndex(citeStartIndex)
            .endIndex(currentIndex)
            .build();
        citeStartIndex = null;
        return new TokenProcessResult(null, citation);
      } else if (content != null) {
        // cite 태그 내부의 텍스트 (인용된 내용)
        log.info("[CITE CONTENT] before={}, len={}, content={}", currentIndex, content.length(), content);
        textBuilder.append(content);
        currentIndex += content.length();
        return new TokenProcessResult(content, null);
      }
    } else if ("/cite/id".equals(path) && content != null) {
      // <id> 태그 내용: 인용 ID 누적 (스트리밍 출력 안함)
      citeIdBuilder.append(content);
    }
    return new TokenProcessResult(null, null);
  }

  /**
   * 토큰 처리 결과를 담는 레코드.
   *
   * @param content  출력할 content (null이면 출력 안함)
   * @param citation 완성된 Citation (null이면 Citation 없음)
   */
  public record TokenProcessResult(String content, Citation citation) {
  }
}
