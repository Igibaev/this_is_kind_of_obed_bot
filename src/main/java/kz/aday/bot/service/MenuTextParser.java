/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import kz.aday.bot.exception.TelegramMessageException;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.util.Messages;
import kz.aday.bot.util.TimeFormatterExtractor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MenuTextParser {
  private static final String LINE_SEPARATOR = "\n";
  private static final int DEADLINE_LINES_COUNT = 1;
  private static final char DOT = '.';

  private MenuTextParser() {}

  public static LocalDateTime parseDeadline(String message) throws TelegramMessageException {
    LocalTime deadline = TimeFormatterExtractor.extractTimes(message);
    if (deadline == null) {
      log.error("Deadline is missing [{}]", deadline);
      throw new TelegramMessageException(
          "Дедлайн некорректный, исправьте сообщение и отправьте заново.");
    }
    if (deadline.isBefore(LocalTime.now())) {
      log.error("Deadline is passed [{}]", deadline);
      throw new TelegramMessageException(
          "Дедлайн "
              + deadline
              + " не может быть в прошлом, исправьте сообщение и отправьте заново.");
    }
    return LocalDateTime.of(LocalDate.now(), deadline);
  }

  public static Menu parseMenu(String message) throws TelegramMessageException {
    Menu menu = new Menu();
    menu.setDeadline(parseDeadline(message));

    List<Item> itemList = new ArrayList<>();
    Category currentCategory = null;
    Set<Category> visitedCategories = EnumSet.noneOf(Category.class);
    int counter = 0;
    for (String rawLine : linesWithoutDeadline(message)) {
      if (rawLine.isBlank()) {
        continue;
      }
      String line = removeDigitsAndDots(rawLine);

      Category category = parseCategory(line);
      if (category != null) {
        markCategoryVisited(visitedCategories, category);
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
    if (itemList.isEmpty()) {
      log.error("Menu is missing");
      throw new TelegramMessageException(
          "Меню пустое. Отправьте исправленный текст согласно шаблону.");
    }
    menu.setItemList(itemList);
    menu.setStatus(Status.PENDING);
    return menu;
  }

  private static void markCategoryVisited(Set<Category> visitedCategories, Category category)
      throws TelegramMessageException {
    if (!visitedCategories.add(category)) {
      log.error("Category is duplicated [{}]", category);
      throw new TelegramMessageException(
          Messages.MENU_CATEGORY_DUPLICATED.getText(category.getValue()));
    }
  }

  private static List<String> linesWithoutDeadline(String message) {
    List<String> lines = List.of(message.split(LINE_SEPARATOR));
    return lines.subList(0, lines.size() - DEADLINE_LINES_COUNT);
  }

  private static String removeDigitsAndDots(String line) {
    StringBuilder stringBuilder = new StringBuilder();
    for (char letter : line.toCharArray()) {
      if (!Character.isDigit(letter) && letter != DOT) {
        stringBuilder.append(letter);
      }
    }
    return stringBuilder.toString().trim();
  }

  private static Category parseCategory(String line) {
    if (line.contains(Category.BREAD.getValue())) {
      return Category.BREAD;
    }
    return Category.from(line);
  }
}
