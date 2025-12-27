package com.saltlux.workflow.direct.payload.messageable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 기본 문서 구현체.
 * <p>
 * {@link IDocument}의 기본 구현으로, id, title, content만 포함하는 단순 문서.
 * </p>
 */
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class SimpleDocument implements IDocument {

  private String id;
  private String title;
  private String content;
  private String url;
}
