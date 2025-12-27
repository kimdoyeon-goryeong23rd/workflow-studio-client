package com.saltlux.workflow.deepresearch.payload;

import com.saltlux.workflow.deepresearch.payload.PrecedentPayloads.PrecedentFilter;
import com.saltlux.workflow.deepresearch.payload.StatutePayloads.StatuteFilter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Self-Query 플로우의 응답 DTO.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class SelfQueryResponse {

  private StatuteFilter statuteFilter;
  private PrecedentFilter precedentFilter;
  private String semanticQuery;
  private Integer baseDate;
}
