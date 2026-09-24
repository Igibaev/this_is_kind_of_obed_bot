/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.List;
import kz.aday.bot.util.Messages;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class MessageSender {
  static final List<String> NAVIGATION_COMMANDS = List.of("/return", "/menu", "/start", "/cancel");

  public Message sendMessage(SendMessage sendMessage, AbsSender absSender)
      throws TelegramApiException {
    return sendMessage(sendMessage, absSender, false);
  }

  public Message sendMessage(
      SendMessage sendMessage, AbsSender absSender, boolean suppressNavigationHint)
      throws TelegramApiException {
    if (!suppressNavigationHint && shouldAppendNavigationHint(sendMessage.getText())) {
      sendMessage.setText(Messages.NAVIGATION_HINT.getText(sendMessage.getText()));
    }
    try {
      return absSender.execute(sendMessage);
    } catch (TelegramApiException e) {
      log.error(
          "Failed to send message user:{}. \nReason: [{}]",
          sendMessage.getChatId(),
          e.getMessage());
      throw e;
    }
  }

  public void deleteMessage(Long chatId, List<Integer> messagesIdList, AbsSender sender) {
    for (Integer messageId : messagesIdList) {
      if (messageId == null) {
        log.debug("User:[{}] prev message is null. skip.", chatId);
        continue;
      }
      DeleteMessage deleteMessage = new DeleteMessage();
      deleteMessage.setChatId(chatId);
      deleteMessage.setMessageId(messageId);
      try {
        sender.executeAsync(deleteMessage);
      } catch (TelegramApiException e) {
        log.debug("Failed to delete message ID: [{}], user:[{}]", messageId, chatId);
      }
    }
  }

  private static boolean shouldAppendNavigationHint(String text) {
    return text != null && NAVIGATION_COMMANDS.stream().noneMatch(text::contains);
  }
}
