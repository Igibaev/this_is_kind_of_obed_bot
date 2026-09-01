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

public class CreateMenuStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.CREATE_MENU.getDisplayName().equals(state);
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
              List.of(
                  new UserButton(
                      CallbackState.CHANGE_MENU.getDisplayName(),
                      CallbackState.CHANGE_MENU.toString()),
                  new UserButton(
                      CallbackState.CLEAR_MENU.getDisplayName(),
                      CallbackState.CLEAR_MENU.toString())),
              markup);
          sendMessageWithKeyboard(
              user,
              Messages.CREATE_MENU_READY.getText(
                  menu.getDeadlineAsText(), user.getCity().getValue()),
              markup,
              getMessageId(update),
              sender);
        } else {
          if (menu.getStatus() == Status.PENDING) {
            InlineKeyboardMarkup markup =
                KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
            KeyboardUtil.addButton(
                List.of(
                    new UserButton(
                        CallbackState.SUBMIT_MENU.getDisplayName(),
                        CallbackState.SUBMIT_MENU.toString()),
                    new UserButton(
                        CallbackState.CHANGE_MENU.getDisplayName(),
                        CallbackState.CHANGE_MENU.toString()),
                    new UserButton(
                        CallbackState.CLEAR_MENU.getDisplayName(),
                        CallbackState.CLEAR_MENU.toString())),
                markup);
            sendMessageWithKeyboard(
                user,
                Messages.CREATE_MENU_PENDING.getText(
                    menu.getDeadlineAsText(), user.getCity().getValue()),
                markup,
                getMessageId(update),
                sender);
          } else {
            user.setState(State.SET_MENU);
            sendMessage(user, Messages.MENU_TEMPLATE.getText(), getMessageId(update), sender);
          }
        }
      } else {
        user.setState(State.SET_MENU);
        sendMessage(user, Messages.MENU_TEMPLATE.getText(), getMessageId(update), sender);
      }
    }
  }
}
