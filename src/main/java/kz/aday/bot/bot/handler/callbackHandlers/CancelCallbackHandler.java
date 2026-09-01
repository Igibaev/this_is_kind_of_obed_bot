/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class CancelCallbackHandler extends AbstractHandler implements CallbackHandler {
  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      user.setState(State.NONE);
      sendMessageWithKeyboard(
          user,
          Messages.NAVIGATION_MENU.getText(),
          getUserMenuKeyboard(user),
          getMessageId(callback),
          sender);
    }
  }

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.CANCEL);
  }
}
