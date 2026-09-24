/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttendanceStat {
  private String chatId;
  private String username;
  private int visits;
}
