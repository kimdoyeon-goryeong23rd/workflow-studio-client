package com.saltlux.workflow.deepresearch.payload.messageable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.saltlux.workflow.direct.payload.chatcompletion.Message;
import com.saltlux.workflow.direct.payload.messageable.AttachedMessage;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResearchedMessage extends AttachedMessage {
  /** 활성화된 에디터의 안의 내용(HTML) */
  private String editor;

  /** 리서치 결과 */
  private ResearchResult researchResult;

  /**
   * 일반 Message로 변환한다.
   * <p>
   * content에 다음 순서로 병합됩니다:
   * editor -> content -> documents -> researchResult
   * </p>
   *
   * @return 변환된 Message
   */
  @Override
  @JsonIgnore
  public Message toMessage() {
    final StringBuilder sb = new StringBuilder();

    // 1. editor
    if (editor != null && !editor.isEmpty()) {
      sb.append("<editor>\n").append(editor).append("\n</editor>");
    }

    // 2. content
    final String contentText = getContent();
    if (contentText != null && !contentText.isEmpty()) {
      if (!sb.isEmpty()) {
        sb.append("\n\n");
      }
      sb.append(contentText);
    }

    // 3. documents
    final String documentsText = serializeDocuments();
    if (!documentsText.isEmpty()) {
      if (!sb.isEmpty()) {
        sb.append("\n\n");
      }
      sb.append(documentsText);
    }

    // 4. researchResult
    final String researchResultText = serializeResearchResult();
    if (!researchResultText.isEmpty()) {
      if (!sb.isEmpty()) {
        sb.append("\n\n");
      }
      sb.append(researchResultText);
    }

    return Message.builder()
        .role(getRole())
        .content(sb.toString())
        .build();
  }

  /**
   * researchResult를 {@code <research-result>} 태그로 감싸서 직렬화한다.
   * <p>
   * 검색된 문서들, reason, plan만 포함합니다.
   * </p>
   */
  @JsonIgnore
  private String serializeResearchResult() {
    if (researchResult == null) {
      return "";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("<research-result>\n");

    // 검색된 문서들
    final var docs = researchResult.getAllDocuments();
    if (docs != null && !docs.isEmpty()) {
      sb.append("<retrieved-documents>\n");
      for (final var doc : docs) {
        sb.append(doc.toSerializedPrompt()).append("\n");
      }
      sb.append("</retrieved-documents>\n");
    }

    if (researchResult.getReason() != null) {
      sb.append("<reason>").append(researchResult.getReason()).append("</reason>\n");
    }

    if (researchResult.getPlan() != null) {
      sb.append("<plan>").append(researchResult.getPlan()).append("</plan>\n");
    }

    sb.append("</research-result>");
    return sb.toString();
  }
}
