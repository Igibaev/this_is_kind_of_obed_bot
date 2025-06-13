/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class ErrorHandler extends AbstractHandler {
  public void handle(Exception e, Update update, AbsSender sender) throws TelegramApiException {
    log.warn("Caught exception [{}]", e.getMessage(), e);
    String chatId = "";

    if (update.getMessage() != null && update.getMessage().getChatId() != null) {
      chatId = getChatId(update).toString();
    } else {
      if (update.hasCallbackQuery()) {
        chatId = getChatId(update.getCallbackQuery()).toString();
      }
    }

    sendMessage(
        userService.findById(chatId),
        String.format(ERROR_MESSAGE, e.getMessage()),
        update.getMessage().getMessageId(),
        sender);
  }

  private static final String ERROR_MESSAGE =
      "Произошла ошибка:%s.\nЧтобы вернуться в меню нажми /return";
}
