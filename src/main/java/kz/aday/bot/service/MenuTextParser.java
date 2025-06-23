/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import kz.aday.bot.exception.TelegramMessageException;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.util.TimeFormatterExtractor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MenuTextParser {

  private MenuTextParser() {}

  public static LocalDateTime parseDeadline(String message) throws TelegramMessageException {
    LocalTime deadline = TimeFormatterExtractor.extractTimes(message);
    if (deadline == null) {
      log.error("Deadline is missing [{}]", message);
      throw new TelegramMessageException("Дедлайн некорректный, " + message);
    }
    return LocalDateTime.of(LocalDate.now(), deadline);
  }

  public static Menu parseMenu(String message) throws TelegramMessageException {
    Menu menu = new Menu();
    menu.setDeadline(parseDeadline(message));

    List<Item> itemList = new ArrayList<>();
    Category currentCategory = null;
    String[] lines = message.split("\n");
    int counter = 0;
    for (String line : lines) {
      if (line.isBlank()) {
        continue;
      }
      line = line.trim();
      line = removeNonLetterCharacters(line);

      Category category = parseCategory(line);
      if (category != null) {
        if (category == Category.BREAD) {
          itemList.add(new Item(counter++, category.getValue(), category));
          continue;
        }
        currentCategory = category;
      } else {
        if (currentCategory != null) {
          Item item = new Item(counter++, line, currentCategory);
          itemList.add(item);
        }
      }
    }
    menu.setItemList(itemList);
    menu.setStatus(Status.PENDING);
    return menu;
  }

  private static String removeNonLetterCharacters(String line) {
    StringBuilder stringBuilder = new StringBuilder();
    for (char letter : line.toCharArray()) {
      if (!Character.isDigit(letter) && letter != '.') {
        stringBuilder.append(letter);
      }
    }
    return stringBuilder.toString().trim();
  }

  private static Category parseCategory(String line) {
    if (line.contains(Category.BREAD.getValue())) return Category.BREAD;
    for (Category category : Category.values()) {
      if (line.toLowerCase().startsWith(category.getValue().toLowerCase())) {
        return category;
      }
    }
    return null;
  }
}
