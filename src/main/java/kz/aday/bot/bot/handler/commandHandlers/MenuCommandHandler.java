/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class MenuCommandHandler extends AbstractHandler implements CommandHandler {
  @Override
  public boolean canHandle(String command) {
    return "/menu".equals(command);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    User user = userService.findById(getChatId(update).toString());
    user.setState(State.NONE);
    sendMessageWithKeyboard(
        user,
        Messages.NAVIGATION_MENU.getText(),
        getUserMenuKeyboard(user),
        getMessageId(update),
        sender);
  }
}
