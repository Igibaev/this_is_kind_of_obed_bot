/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.time.LocalDate;

public interface Id {
  String getId();

  default LocalDate getStorageDate() {
    return LocalDate.now();
  }
}
