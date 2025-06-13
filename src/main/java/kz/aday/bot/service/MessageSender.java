/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class MessageSender {

  public Message sendMessage(SendMessage sendMessage, AbsSender absSender)
      throws TelegramApiException {
    return absSender.execute(sendMessage);
  }

  public void deleteMessage(Long chatId, List<Integer> messagesIdList, AbsSender sender) {
    if (!messagesIdList.isEmpty()) {
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
          log.debug("Fail to deleted message ID: [{}], user:[{}]", messageId, chatId);
        }
      }
    }
  }
}
