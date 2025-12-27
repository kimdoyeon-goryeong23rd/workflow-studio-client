package com.saltlux.workflow.direct.payload.messageable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.saltlux.workflow.deepresearch.payload.PrecedentPayloads;
import com.saltlux.workflow.deepresearch.payload.StatutePayloads;

/**
 * RAG용 문서 인터페이스.
 * <p>
 * LLM 요청 시 참조 문서로 첨부될 수 있는 문서를 나타낸다.
 * {@link #toSerializedPrompt()}를 통해 프롬프트에 삽입될 문자열로 변환된다.
 * </p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = SimpleDocument.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SimpleDocument.class, name = "document"),
    @JsonSubTypes.Type(value = StatutePayloads.StatuteChunk.class, name = "statute"),
    @JsonSubTypes.Type(value = PrecedentPayloads.PrecedentChunk.class, name = "precedent")
})
public interface IDocument {

  /**
   * 문서 고유 ID.
   * <p>
   * LLM 응답에서 cite 태그의 id로 사용된다.
   * </p>
   *
   * @return 문서 ID
   */
  String getId();

  /**
   * 문서 제목.
   *
   * @return 문서 제목
   */
  String getTitle();

  /**
   * 문서 내용.
   *
   * @return 문서 내용
   */
  String getContent();

  /**
   * 문서 출처 URL.
   * <p>
   * 기본 구현은 null을 반환한다.
   * 구현체에서 고유 필드를 기반으로 URL을 생성할 수 있다.
   * </p>
   *
   * @return 문서 URL 또는 null
   */
  default String getUrl() {
    return null;
  }

  /**
   * 문서를 프롬프트에 삽입할 수 있는 문자열로 변환한다.
   * <p>
   * 기본 구현은 {@code <document>} 태그로 감싸서 id, title, content를 포함한다.
   * </p>
   *
   * @return 직렬화된 프롬프트 문자열
   */
  default String toSerializedPrompt() {
    final StringBuilder sb = new StringBuilder();
    sb.append("<document id=\"").append(getId()).append("\">\n");
    if (getTitle() != null && !getTitle().isBlank()) {
      sb.append("<title>").append(getTitle()).append("</title>\n");
    }
    sb.append("<content>").append(getContent()).append("</content>\n");
    sb.append("</document>");
    return sb.toString();
  }
}
