package com.saltlux.workflow.direct.payload.messageable;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.saltlux.workflow.deepresearch.payload.messageable.ResearchedMessage;
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
 * 문서가 첨부된 메시지.
 * <p>
 * 기본 메시지 필드(role, content)에 추가로 참조 문서 목록을 포함합니다.
 * {@link #toMessage()}를 통해 documents가 content에 병합된 일반 Message로 변환됩니다.
 * </p>
 *
 * @see IDocument
 * @see Message
 */
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes.Type(ResearchedMessage.class)
public class AttachedMessage implements IMessageable {

  private String role;

  /** 첨부된 참조 문서 목록 */
  @Builder.Default
  private List<IDocument> documents = new ArrayList<>();

  private String content;

  /**
   * 일반 Message로 변환한다.
   * <p>
   * documents가 있으면 content 앞에 {@code <documents>} 태그로 감싸서 첨부합니다.
   * </p>
   *
   * @return 변환된 Message
   */
  @Override
  @JsonIgnore
  public Message toMessage() {
    final String documentsText = serializeDocuments();

    String mergedContent;
    if (documentsText.isEmpty()) {
      mergedContent = content;
    } else if (content == null || content.isEmpty()) {
      mergedContent = documentsText;
    } else {
      mergedContent = content + "\n\n" + documentsText;
    }

    return Message.builder()
        .role(role)
        .content(mergedContent)
        .build();
  }

  /**
   * documents를 {@code <documents>} 태그로 감싸서 직렬화한다.
   */
  @JsonIgnore
  public String serializeDocuments() {
    if (this.documents == null || this.documents.isEmpty()) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    sb.append("<documents>\n");
    for (final IDocument doc : this.documents) {
      sb.append(doc.toSerializedPrompt()).append("\n");
    }
    sb.append("</documents>");
    return sb.toString();
  }
}
