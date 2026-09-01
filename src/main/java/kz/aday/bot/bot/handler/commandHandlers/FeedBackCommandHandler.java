/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class FeedBackCommandHandler extends AbstractHandler implements CommandHandler {
  @Override
  public boolean canHandle(String command) {
    return command.startsWith("/feedback");
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    User user = userService.findById(getChatId(update).toString());
    user.setState(State.SEND_FEEDBACK);
    sendMessage(user, Messages.FEEDBACK_FORM.getText(), user.getLastMessageId(), sender);
  }
}
