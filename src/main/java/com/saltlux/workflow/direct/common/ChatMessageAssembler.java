package com.saltlux.workflow.direct.common;

import java.util.List;

import com.saltlux.workflow.direct.payload.messageable.ResponseMessage;

/**
 * 기본 Chat Completion 스트리밍 응답용 메시지 어셈블러.
 *
 * <p>
 * cite 태그 파싱 없이 content, reasoning, toolCalls를 단순 누적하여
 * {@link ResponseMessage}를 생성한다.
 * </p>
 */
public class ChatMessageAssembler extends MessageAssembler<ResponseMessage> {

  @Override
  public List<ResponseMessage> processDelta(final ResponseMessage delta) {
    if (delta == null) {
      return List.of();
    }

    appendContent(delta.getContent());
    appendReasoning(delta.getReasoning());
    mergeToolCalls(delta.getToolCalls());

    // delta에 내용이 있으면 그대로 반환
    if (delta.getContent() != null || delta.getReasoning() != null || delta.getToolCalls() != null) {
      return List.of(ResponseMessage.builder()
          .role(delta.getRole())
          .content(delta.getContent())
          .reasoning(delta.getReasoning())
          .toolCalls(delta.getToolCalls())
          .build());
    }
    return List.of();
  }

  @Override
  public ResponseMessage buildFinalMessage() {
    return ResponseMessage.builder()
        .role("assistant")
        .content(getContent())
        .reasoning(getReasoning())
        .toolCalls(getToolCalls())
        .build();
  }
}
