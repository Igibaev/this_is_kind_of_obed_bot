/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ClearMenuStateHandler extends AbstractHandler implements StateHandler {

  @Override
  public boolean canHandle(String state) {
    return State.CLEAR_MENU.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (checkAdminRole(user, getMessageId(update), sender)) {
        return;
      }
      if (isMenuExist(user.getCity())) {
        sendMessageWithKeyboard(
            user,
            Messages.MENU_TO_DELETE.getText(),
            KeyboardUtil.createInlineKeyboard(
                List.of(
                    new UserButton("Удалить меню", CallbackState.CLEAR_MENU.toString()),
                    new UserButton("Отменить", CallbackState.CANCEL.toString()))),
            getMessageId(update),
            sender);
      } else {
        sendMessage(user, Messages.MENU_NOT_EXIST_FOR_CITY.getText(), getMessageId(update), sender);
      }
    }
  }
}
