package com.saltlux.workflow.direct.payload.messageable;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.saltlux.workflow.direct.payload.chatcompletion.Message;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 인용 정보가 포함된 응답 메시지.
 * <p>
 * 기본 메시지 필드(role, content)에 추가로 reasoning, toolCalls, citations를 포함합니다.
 * </p>
 *
 * @see IMessageable
 * @see Citation
 */
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CitedMessage extends ResponseMessage {
  @Builder.Default
  private List<Citation> citations = new ArrayList<>();

  /**
   * 인용 정보를 cite 태그 형태로 직렬화하여 반환한다.
   * <p>
   * 예: {@code 일반 텍스트<cite><id>doc1</id>인용된 텍스트</cite>일반 텍스트}
   * </p>
   *
   * @return cite 태그가 포함된 문자열
   */
  @JsonIgnore
  public String getContentWithCitations() {
    if (super.getContent() == null || super.getContent().isEmpty()) {
      return "";
    }
    if (citations == null || citations.isEmpty()) {
      return super.getContent();
    }

    // citations를 startIndex 기준으로 정렬
    final List<Citation> sorted = citations.stream()
        .sorted((a, b) -> Integer.compare(a.getStartIndex(), b.getStartIndex()))
        .toList();

    final StringBuilder sb = new StringBuilder();
    int lastEnd = 0;

    for (final Citation cite : sorted) {
      // 인용 전 일반 텍스트
      if (cite.getStartIndex() > lastEnd) {
        sb.append(super.getContent(), lastEnd, cite.getStartIndex());
      }

      // 인용 부분을 cite 태그로 감싸기
      sb.append("<cite><id>").append(cite.getId()).append("</id>");
      sb.append(super.getContent(), cite.getStartIndex(), cite.getEndIndex());
      sb.append("</cite>");

      lastEnd = cite.getEndIndex();
    }

    // 마지막 인용 이후 텍스트
    if (lastEnd < super.getContent().length()) {
      sb.append(super.getContent().substring(lastEnd));
    }

    return sb.toString();
  }

  /**
   * 일반 Message로 변환한다.
   * <p>
   * 인용 정보가 있으면 cite 태그가 포함된 content로 변환된다.
   * </p>
   *
   * @return Message
   */
  @Override
  public Message toMessage() {
    return Message.builder()
        .role(super.getRole())
        .content(getContentWithCitations())
        .build();
  }

}
