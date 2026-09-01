/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import static kz.aday.bot.util.Messages.RETURN_TO_MENU;

import kz.aday.bot.exception.TelegramMessageException;
import lombok.Getter;

@Getter
public enum City {
  ASTANA("Астана", false),
  ALMATA("Алматы", true),
  KARAGANDA("Караганда", false);

  private final String value;
  private final boolean nextDayOrderCycle;

  City(String value, boolean nextDayOrderCycle) {
    this.value = value;
    this.nextDayOrderCycle = nextDayOrderCycle;
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
