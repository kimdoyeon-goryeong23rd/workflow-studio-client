package com.saltlux.workflow.direct.payload.messageable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.saltlux.workflow.direct.payload.chatcompletion.IMessage;
import com.saltlux.workflow.direct.payload.chatcompletion.Message;

/**
 * Message로 변환 가능한 객체를 나타내는 인터페이스.
 * <p>
 * LLM API 요청 시 다양한 메시지 타입({@link AttachedMessage}, {@link ResponseMessage} 등)을
 * 표준 {@link Message} 형태로 변환하여 전송할 수 있게 합니다.
 * </p>
 *
 * @see Message
 * @see AttachedMessage
 * @see ResponseMessage
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = BaseMessage.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = BaseMessage.class),
    @JsonSubTypes.Type(value = AttachedMessage.class),
    @JsonSubTypes.Type(value = ResponseMessage.class),
    @JsonSubTypes.Type(value = CitedMessage.class)
})
public interface IMessageable extends IMessage {
  /**
   * LLM API 요청용 표준 Message로 변환한다.
   *
   * @return 변환된 Message
   */
  Message toMessage();
}
