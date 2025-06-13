/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import static kz.aday.bot.messages.Messages.RETURN_TO_MENU;

import kz.aday.bot.exception.TelegramMessageException;
import lombok.Getter;

@Getter
public enum City {
  ASTANA("Астана"),
  ALMATA("Алмата");

  private final String value;

  City(String value) {
    this.value = value;
  }

  public static City from(String text) throws TelegramMessageException {
    for (City city : City.values()) {
      if (city.value.equals(text)) {
        return city;
      }
    }
    throw new TelegramMessageException("Введенный город не найден. " + RETURN_TO_MENU);
  }
}
