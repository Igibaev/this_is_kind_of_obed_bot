/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedOrderItem {
  private String entryId;
  private Item item;
  private String sourceChatId;
  private String sourceUsername;
  private String claimedByChatId;
  private String claimedByUsername;
}
