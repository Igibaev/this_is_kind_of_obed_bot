/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;

public interface Id {
  @JsonIgnore
  String getId();

  @JsonIgnore
  default LocalDate getStorageDate() {
    return LocalDate.now();
  }

  @JsonIgnore
  default void backfillDateIfMissing(LocalDate folderDate) {}
}
