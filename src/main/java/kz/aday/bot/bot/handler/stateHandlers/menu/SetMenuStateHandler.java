/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.service.MenuTextParser;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SetMenuStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.SET_MENU.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (checkAdminRole(user, getMessageId(update), sender)) {
        return;
      }
      Menu menu = null;
      try {
        menu = MenuTextParser.parseMenu(update.getMessage().getText());
      } catch (Exception e) {
        sendMessage(user, e.getMessage(), getMessageId(update), sender);
        return;
      }
      menu.setCity(user.getCity());
      menuService.save(menu);

      user.setState(State.NONE);
      InlineKeyboardMarkup markup =
          KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
      KeyboardUtil.addButton(
          List.of(
              new UserButton("Опубликовать", CallbackState.SUBMIT_MENU.toString()),
              new UserButton("Изменить", CallbackState.CHANGE_MENU.toString())),
          markup);
      sendMessageWithKeyboard(
          user,
          Messages.MENU_PENDING.getText(user.getCity().getValue()),
          markup,
          getMessageId(update),
          sender);
    }
  }
}
