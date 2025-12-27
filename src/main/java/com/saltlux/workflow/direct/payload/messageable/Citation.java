package com.saltlux.workflow.direct.payload.messageable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 인용 정보.
 *
 * <p>
 * 텍스트 내에서 인용된 부분의 위치와 인용 ID를 나타냅니다.
 * </p>
 */
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class Citation {
  private int index;
  private String id;
  private int startIndex;
  private int endIndex;
}
