package com.saltlux.workflow.direct.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saltlux.workflow.direct.payload.chatcompletion.ToolCall;
import com.saltlux.workflow.direct.payload.chatcompletion.ToolFunction;
import com.saltlux.workflow.direct.payload.messageable.ResponseMessage;

/**
 * LLM 스트리밍 응답에서 content, reasoning, toolCalls를 누적하여 메시지를 생성하는 추상 어셈블러.
 *
 * <p>
 * 스트리밍 중:
 * <ul>
 * <li>{@link #appendContent}로 content 누적</li>
 * <li>{@link #appendReasoning}으로 reasoning 누적</li>
 * <li>{@link #mergeToolCalls}로 toolCalls 누적</li>
 * </ul>
 * 스트림 종료 시:
 * <ul>
 * <li>{@link #buildFinalMessage}로 최종 메시지 생성</li>
 * </ul>
 * </p>
 *
 * @param <T> 생성할 메시지 타입 (ResponseMessage 이상)
 */
public abstract class MessageAssembler<T extends ResponseMessage> {

  // 텍스트 누적
  protected final StringBuilder textBuilder = new StringBuilder();

  // reasoning 누적
  protected final StringBuilder reasoningBuilder = new StringBuilder();

  // index별 ToolCall 빌더 (스트리밍에서 arguments가 청크로 옴)
  private final Map<Integer, ToolCallBuilder> toolCallBuilders = new HashMap<>();

  /**
   * content를 누적한다.
   *
   * @param content 추가할 content 텍스트
   */
  protected void appendContent(final String content) {
    if (content != null) {
      textBuilder.append(content);
    }
  }

  /**
   * reasoning을 누적한다.
   *
   * @param reasoning 추가할 reasoning 텍스트
   */
  protected void appendReasoning(final String reasoning) {
    if (reasoning != null) {
      reasoningBuilder.append(reasoning);
    }
  }

  /**
   * ToolCall 청크를 처리하여 index 기반으로 누적한다.
   *
   * <p>
   * 스트리밍에서 같은 index의 ToolCall이 여러 청크로 나뉘어 옴:
   * <ul>
   * <li>첫 청크: id, type, function.name 포함</li>
   * <li>이후 청크: function.arguments만 포함 (append 필요)</li>
   * </ul>
   * </p>
   *
   * @param calls ToolCall 청크 리스트
   */
  protected void mergeToolCalls(final List<ToolCall> calls) {
    if (calls == null) {
      return;
    }
    for (final ToolCall call : calls) {
      if (call.getIndex() == null) {
        continue;
      }
      toolCallBuilders.computeIfAbsent(call.getIndex(), ToolCallBuilder::new)
          .merge(call);
    }
  }

  /**
   * delta를 처리하여 누적하고, 스트리밍용 메시지 리스트를 반환한다.
   *
   * <p>
   * 각 delta의 content, reasoning, toolCalls를 누적하고,
   * 해당 delta에 대한 스트리밍용 메시지를 생성하여 반환한다.
   * </p>
   *
   * @param delta 처리할 delta (nullable)
   * @return 스트리밍용 메시지 리스트 (delta가 null이거나 내용이 없으면 빈 리스트)
   */
  public abstract List<T> processDelta(ResponseMessage delta);

  /**
   * 최종 메시지를 생성한다.
   *
   * @return 누적된 content, reasoning, toolCalls가 포함된 최종 메시지
   */
  public abstract T buildFinalMessage();

  /**
   * 누적된 content를 반환한다.
   */
  protected String getContent() {
    return textBuilder.toString();
  }

  /**
   * 누적된 reasoning을 반환한다. 비어있으면 null.
   */
  protected String getReasoning() {
    return reasoningBuilder.isEmpty() ? null : reasoningBuilder.toString();
  }

  /**
   * 누적된 ToolCall들을 index 순서로 정렬하여 리스트로 반환한다. 비어있으면 null.
   */
  protected List<ToolCall> getToolCalls() {
    final List<ToolCall> toolCalls = toolCallBuilders.values().stream()
        .sorted((a, b) -> Integer.compare(a.index, b.index))
        .map(ToolCallBuilder::build)
        .toList();
    return toolCalls.isEmpty() ? null : toolCalls;
  }

  /**
   * ToolCall 스트리밍 청크를 누적하기 위한 빌더.
   */
  private static class ToolCallBuilder {
    final int index;
    String id;
    String type;
    String name;
    final StringBuilder arguments = new StringBuilder();

    ToolCallBuilder(final int index) {
      this.index = index;
    }

    void merge(final ToolCall chunk) {
      if (chunk.getId() != null) {
        this.id = chunk.getId();
      }
      if (chunk.getType() != null) {
        this.type = chunk.getType();
      }
      if (chunk.getFunction() != null) {
        final ToolFunction func = chunk.getFunction();
        if (func.getName() != null) {
          this.name = func.getName();
        }
        if (func.getArguments() != null) {
          this.arguments.append(func.getArguments());
        }
      }
    }

    ToolCall build() {
      return ToolCall.builder()
          .index(index)
          .id(id)
          .type(type)
          .function(ToolFunction.builder()
              .name(name)
              .arguments(arguments.toString())
              .build())
          .build();
    }
  }
}
