package com.saltlux.workflow.direct.payload.chatcompletion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saltlux.workflow.direct.DirectLlmProcessor;
import com.saltlux.workflow.direct.payload.DirectLlmRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * OpenAI Chat Completion API 요청 객체.
 *
 * <p>
 * OpenAI 호환 API의 표준 요청 형식을 따릅니다.
 * </p>
 *
 * @see DirectLlmProcessor
 * @see DirectLlmRequest
 */
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest extends BaseCompletionRequest<Message> {
}
