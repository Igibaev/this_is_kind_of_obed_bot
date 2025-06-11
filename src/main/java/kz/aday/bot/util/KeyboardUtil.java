package kz.aday.bot.util;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.UserButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class KeyboardUtil {

    public static ReplyKeyboardMarkup createReplyKeyboard(Collection<String> buttons) {
        // Создаём ReplyKeyboardMarkup
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Автоматический размер кнопок
        keyboardMarkup.setOneTimeKeyboard(false); // Клавиатура остаётся после нажатия

        List<KeyboardRow> keyboard = new ArrayList<>();
        final int MAX_ROW_LENGTH = 30; // Максимальная длина текста в строке (в символах)
        KeyboardRow currentRow = new KeyboardRow();
        int currentRowLength = 0;

        for (String buttonText : buttons) {
            int buttonLength = buttonText.length();

            // Если кнопка слишком длинная или строка переполнена, начинаем новую строку
            if (currentRowLength + buttonLength > MAX_ROW_LENGTH || buttonLength > 15) {
                if (!currentRow.isEmpty()) {
                    keyboard.add(currentRow);
                }
                currentRow = new KeyboardRow();
                currentRowLength = 0;
            }

            currentRow.add(buttonText);
            currentRowLength += buttonLength;

            // Если строка заполнена, добавляем её в клавиатуру
            if (currentRow.size() >= 3 || currentRowLength >= MAX_ROW_LENGTH) {
                keyboard.add(currentRow);
                currentRow = new KeyboardRow();
                currentRowLength = 0;
            }
        }

        // Добавляем последнюю строку, если она не пуста
        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup createInlineKeyboard(List<Item> itemList, CallbackState state) {
        return createInlineKeyboard(itemList, Collections.emptySet(), state);
    }

    public static InlineKeyboardMarkup createInlineKeyboard(List<Item> itemList, Set<Item> selectedItems, CallbackState state) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Item item : itemList) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(getTextButton(selectedItems, item));
            button.setCallbackData(state + ":" + item.getId());
            keyboard.add(List.of(button));
        }

        markup.setKeyboard(keyboard);
        return markup;
    }

    private static String getTextButton(Set<Item> selectedItems, Item item) {
        return item.getCategory().getDisplayName() + ": " + (selectedItems.contains(item) ? " ✅" : "") + item.getName();
    }

    public static void addButton(List<UserButton> userButtons, InlineKeyboardMarkup markup) {
        for (UserButton userButton: userButtons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(userButton.getName());
            button.setCallbackData(userButton.getCallback());
            markup.getKeyboard().add(List.of(button));
        }
    }
}
