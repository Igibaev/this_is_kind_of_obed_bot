/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import kz.aday.bot.bot.handler.stateHandlers.State;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements Id {
  private Long chatId;
  private String preferedName;
  private Integer lastMessageId;
  private City city;
  private Role role;
  private State state;
  private Status status;

  @Override
  public String getId() {
    return chatId.toString();
  }

  public enum Role {
    ADMIN,
    USER;
  }
}
