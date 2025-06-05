package kz.aday.bot.service;

import kz.aday.bot.exception.TelegramMessageException;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.util.TimeFormatterExtractor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MenuParser {

    public LocalDateTime parseDeadline(String message) throws TelegramMessageException {
        LocalTime deadline = TimeFormatterExtractor.extractTimes(message);
        if (deadline == null) {
            log.error("Deadline is missing [{}]", message);
            throw new TelegramMessageException("");
        }
        return LocalDateTime.of(LocalDate.now(), deadline);
    }

    public Menu parseMenu(String message) throws TelegramMessageException {
        Menu menu = new Menu();
        menu.setDeadline(parseDeadline(message));

        List<Item> itemList = new ArrayList<>();
        Category currentCategory = null;
        String[] lines = message.split("\n");
        int counter = 0;
        for (String line: lines) {
            if (line.isBlank()) {
                continue;
            }
            line = line.trim();
            line = removeNonLetterCharacters(line);

            Category category = parseCategory(line);
            if (category != null) {
                if (category == Category.BREAD) {
                    itemList.add(new Item(counter++, "", category));
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

    private String removeNonLetterCharacters(String line) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char letter: line.toCharArray()) {
            if (Character.isLetter(letter)) {
                // TODO: 03.06.2025 добавить уборку числе из текста
                stringBuilder.append(" ");
            }
        }
        return stringBuilder.toString();
    }

    private Category parseCategory(String line) {
        if (line.contains(Category.BREAD.getValue())) return Category.BREAD;
        for (Category category: Category.values()) {
            if (line.toLowerCase().startsWith(category.getValue().toLowerCase())) {
                return category;
            }
        }
        return null;
    }

}
