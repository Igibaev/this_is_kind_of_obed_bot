/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.time.LocalDate;
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
  private String date;

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
        + ", date="
        + date
        + '}';
  }

  @Override
  public String getId() {
    return chatId + "_" + date;
  }

  @Override
  public LocalDate getStorageDate() {
    return LocalDate.parse(date);
  }
}
