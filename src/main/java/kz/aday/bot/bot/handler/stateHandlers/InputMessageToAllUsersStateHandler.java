/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.util.List;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class InputMessageToAllUsersStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.SEND_MESSAGE_TO_ALL_USERS.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
      } else {
        if (user.getState() == State.SEND_MESSAGE_TO_ALL_USERS) {
          Message message = update.getMessage();

          List<User> recipients =
                  userService.findAll().stream()
                          .filter(u -> u.getCity() == user.getCity())
                          .collect(Collectors.toList());
          boolean hasContent =
                  message.hasText()
                          || message.hasPhoto()
                          || message.hasVideo()
                          || message.hasAudio()
                          || message.hasDocument()
                          || message.hasVoice()
                          || message.hasAnimation()
                          || message.hasSticker();

          if (!hasContent) {
            log.info("No message found");
            return;
          }

          for (User recipient : recipients) {
            try {
              ForwardMessage forward = new ForwardMessage();
              forward.setChatId(recipient.getChatId());
              forward.setFromChatId(message.getChatId().toString());
              forward.setMessageId(message.getMessageId());
              forward.setProtectContent(true);
              sender.executeAsync(forward);
            } catch (TelegramApiException e) {
              log.error("Failed send message to user:{}. Reason: {}", user.getId(), e.getMessage());
            }
          }
          SendMessage confirm = new SendMessage();
          confirm.setChatId(message.getChatId().toString());
          confirm.setText("Рассылка прошла успешно.");
          sender.executeAsync(confirm);
          user.setState(State.NONE);
          userService.save(user);
        } else {
          user.setState(State.SEND_MESSAGE_TO_ALL_USERS);
          sendMessage(
              user,
              String.format(INPUT_MESSAGE, user.getCity().getValue()),
              getMessageId(update),
              sender);
        }
      }
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";

  private static final String INPUT_MESSAGE =
      "Введите сообщение для рассылки, для города %s. \n" + "Чтобы отменить нажмите /cancel";
}
