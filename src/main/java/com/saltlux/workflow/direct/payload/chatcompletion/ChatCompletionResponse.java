package com.saltlux.workflow.direct.payload.chatcompletion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.saltlux.workflow.direct.payload.messageable.ResponseMessage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse extends BaseCompletionResponse<ResponseMessage> {

}
