/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class PublishMenuStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.PUBLISH_MENU.getDisplayName().equals(state);
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
        Menu menu = menuService.findById(user.getCity().toString());
        if (menu.getStatus() == Status.READY) {
          InlineKeyboardMarkup markup =
              KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
          KeyboardUtil.addButton(
              List.of(new UserButton("Изменить меню", CallbackState.CHANGE_MENU.toString())),
              markup);
          sendMessageWithKeyboard(
              user,
              Messages.PUBLISH_MENU_READY.getText(
                  menu.getDeadlineAsText(), user.getCity().getValue()),
              markup,
              getMessageId(update),
              sender);
        } else {
          InlineKeyboardMarkup markup =
              KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
          KeyboardUtil.addButton(
              List.of(
                  new UserButton("Опубликовать меню", CallbackState.SUBMIT_MENU.toString()),
                  new UserButton("Изменить меню", CallbackState.CHANGE_MENU.toString()),
                  new UserButton("Удалить меню", CallbackState.CLEAR_MENU.toString())),
              markup);
          sendMessageWithKeyboard(
              user,
              Messages.PUBLISH_MENU.getText(menu.getDeadlineAsText(), user.getCity().getValue()),
              markup,
              getMessageId(update),
              sender);
        }
      } else {
        sendMessage(user, Messages.MENU_NOT_EXIST.getText(), getMessageId(update), sender);
      }
    }
  }
}
