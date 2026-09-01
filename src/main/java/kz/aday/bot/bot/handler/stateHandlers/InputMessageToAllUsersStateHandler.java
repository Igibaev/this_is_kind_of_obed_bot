/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
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
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (checkAdminRole(user, getMessageId(update), sender)) {
        return;
      }
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
        confirm.setText(Messages.BROADCAST_SUCCESS.getText());
        sender.executeAsync(confirm);
        user.setState(State.NONE);
        userService.save(user);
      } else {
        user.setState(State.SEND_MESSAGE_TO_ALL_USERS);
        sendMessage(
            user,
            Messages.BROADCAST_INPUT.getText(user.getCity().getValue()),
            getMessageId(update),
            sender);
      }
    }
  }
}
