/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ProfileStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.PROFILE.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExist(update)) {
      User user = userService.findById(getChatId(update).toString());
      sendMessage(
          user,
          Messages.PROFILE_MESSAGE.getText(user.getPreferedName(), user.getCity().getValue()),
          getMessageId(update),
          sender);
    }
  }
}
