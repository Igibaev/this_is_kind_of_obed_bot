/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficeAttendance implements Id {
  private String chatId;
  private String username;
  private City city;
  private Boolean willCome;

  @Override
  public String toString() {
    return "OfficeAttendance{"
        + "chatId='"
        + chatId
        + '\''
        + ", username='"
        + username
        + '\''
        + ", city="
        + city
        + ", willCome="
        + willCome
        + '}';
  }

  @Override
  public String getId() {
    return chatId;
  }
}
