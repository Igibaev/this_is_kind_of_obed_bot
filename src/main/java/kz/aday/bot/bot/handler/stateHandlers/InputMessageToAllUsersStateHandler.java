package kz.aday.bot.bot.handler.stateHandlers;

import java.util.List;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

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
          List<User> allUsers =
              userService.findAll().stream()
                  .filter(u -> u.getCity() == user.getCity())
                  .collect(Collectors.toList());
          for (User u : allUsers) {
            sendMessage(u, update.getMessage().getText(), getMessageId(update), sender);
          }
        } else {
          user.setState(State.SEND_MESSAGE_TO_ALL_USERS);
          sendMessage(user, INPUT_MESSAGE, getMessageId(update), sender);
        }
      }
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";

  private static final String INPUT_MESSAGE =
      "Введите сообщение для рассылки, для города %s. \n" + "Чтобы отменить нажмите /cancel";
}
