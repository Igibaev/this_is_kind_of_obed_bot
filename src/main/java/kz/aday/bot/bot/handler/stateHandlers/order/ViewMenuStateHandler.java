/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ViewMenuStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.VIEW_MENU_TODAY.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (!isMenuExist(user.getCity())) {
        sendMessage(user, Messages.MENU_IS_NOT_READY_TODAY.getText(), getMessageId(update), sender);
        return;
      }
      Menu menu = menuService.findById(user.getCity().toString());
      sendMessageWithKeyboard(
          user,
          Messages.VIEW_MENU_TODAY_HEADER.getText() + menu.getMenuAsFormattedText(),
          KeyboardUtil.createReplyKeyboard(
              List.of(State.CREATE_ORDER.getDisplayName(), State.BACK_TO_MENU.getDisplayName())),
          getMessageId(update),
          sender,
          true);
    }
  }
}
