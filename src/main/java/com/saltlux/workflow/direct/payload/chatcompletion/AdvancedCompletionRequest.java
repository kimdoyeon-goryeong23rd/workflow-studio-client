package com.saltlux.workflow.direct.payload.chatcompletion;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.saltlux.workflow.direct.DirectLlmProcessor;
import com.saltlux.workflow.direct.payload.messageable.AttachedMessage;
import com.saltlux.workflow.direct.payload.messageable.IMessageable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 확장된 Chat Completion 요청 객체.
 * <p>
 * {@link IMessageable}을 지원하여 {@link AttachedMessage} 등 다양한 메시지 타입을 포함할 수 있습니다.
 * LLM API 호출 시 {@link #toChatCompletionRequest()}를 통해 표준 요청으로 변환됩니다.
 * </p>
 *
 * @see DirectLlmProcessor
 * @see ChatCompletionRequest
 * @see IMessageable
 */
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvancedCompletionRequest extends BaseCompletionRequest<IMessageable> {
  /**
   * LLM API 호출용 표준 ChatCompletionRequest로 변환한다.
   * <p>
   * messages의 각 IMessageable을 Message로 변환하고,
   * stream을 true로 설정합니다.
   * </p>
   *
   * @return 변환된 ChatCompletionRequest
   */
  @JsonIgnore
  public ChatCompletionRequest toChatCompletionRequest() {
    final List<Message> convertedMessages = new ArrayList<>();
    if (this.getMessages() != null) {
      for (final IMessageable msg : this.getMessages()) {
        convertedMessages.add(msg.toMessage());
      }
    }

    return ChatCompletionRequest.builder()
        .model(this.getModel())
        .messages(convertedMessages)
        .temperature(this.getTemperature())
        .topP(this.getTopP())
        .n(this.getN())
        .stream(true)
        .stop(this.getStop())
        .maxTokens(this.getMaxTokens())
        .presencePenalty(this.getPresencePenalty())
        .frequencyPenalty(this.getFrequencyPenalty())
        .logprobs(this.getLogprobs())
        .topLogprobs(this.getTopLogprobs())
        .user(this.getUser())
        .tools(this.getTools())
        .toolChoice(this.getToolChoice())
        .responseFormat(this.getResponseFormat())
        .seed(this.getSeed())
        .build();
  }
}
