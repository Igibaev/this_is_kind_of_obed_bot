/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ChangeOrdersCommandHandler extends AbstractHandler implements CommandHandler {
  @Override
  public boolean canHandle(String command) {
    return "/changeorders".equals(command);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (checkAdminRole(user, getMessageId(update), sender)) {
        return;
      }
    }
  }
}
