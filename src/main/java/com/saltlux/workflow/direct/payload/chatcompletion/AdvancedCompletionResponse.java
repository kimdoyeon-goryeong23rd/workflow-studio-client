package com.saltlux.workflow.direct.payload.chatcompletion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.saltlux.workflow.direct.payload.messageable.CitedMessage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdvancedCompletionResponse extends BaseCompletionResponse<CitedMessage> {

}
