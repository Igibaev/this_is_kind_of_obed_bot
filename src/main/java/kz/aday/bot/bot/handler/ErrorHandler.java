/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import kz.aday.bot.configuration.BotConfig;
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
    if (!isUserExistAndReady(update)) {
      sendMessage(
          userService.findById(chatId),
          GO_TO_START_COMMAND,
          update.hasCallbackQuery()
              ? getMessageId(update.getCallbackQuery())
              : getMessageId(update),
          sender);
      return;
    }

    sendMessage(
        userService.findById(chatId),
        String.format(ERROR_MESSAGE, e.getMessage()),
        update.hasCallbackQuery() ? getMessageId(update.getCallbackQuery()) : getMessageId(update),
        sender);

    sendMessage(
        userService.findById(BotConfig.getMainUserChatId()),
        String.format(ERROR_MESSAGE, e.getMessage()),
        update.hasCallbackQuery() ? getMessageId(update.getCallbackQuery()) : getMessageId(update),
        sender);
  }

  private static final String ERROR_MESSAGE = "Произошла ошибка: %s.";
  private static final String GO_TO_START_COMMAND =
      "Чтобы начать взаимодейcтвовать с ботом, завершите команду /start.";
}
