/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CountMismatch {
  private LocalDate date;
  private int sourceCount;
  private int destinationCount;
}
