package com.saltlux.workflow.direct.payload.messageable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.saltlux.workflow.direct.payload.chatcompletion.Message;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * IMessageable의 기본 구현체.
 * <p>
 * 단순히 role과 content만 가지는 메시지로, JSON 역직렬화 시 기본 타입으로 사용됩니다.
 * </p>
 */
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseMessage implements IMessageable {

  private String role;
  private String content;

  @Override
  public Message toMessage() {
    return Message.builder()
        .role(role)
        .content(content)
        .build();
  }
}
