/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.time.LocalDate;

public interface Id {
  String COMPOSITE_ID_SEPARATOR = "_";

  String getId();

  default LocalDate getStorageDate() {
    return LocalDate.now();
  }

  static String composeId(Object owner, Object date) {
    return owner + COMPOSITE_ID_SEPARATOR + date;
  }

  static String ownerOf(String compositeId) {
    return compositeId.substring(0, compositeId.indexOf(COMPOSITE_ID_SEPARATOR));
  }
}
