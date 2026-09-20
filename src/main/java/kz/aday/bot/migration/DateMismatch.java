/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DateMismatch {
  private String id;
  private LocalDate folderDate;
  private LocalDate fieldDate;
}
