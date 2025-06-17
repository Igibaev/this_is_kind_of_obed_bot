/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import java.util.List;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
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
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
      } else {
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
                String.format(MENU_READY_MESSAGE, user.getCity().getValue()),
                markup,
                getMessageId(update),
                sender);
          } else {
            InlineKeyboardMarkup markup =
                KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
            KeyboardUtil.addButton(
                List.of(
                    new UserButton("Опубликовать меню", CallbackState.SUBMIT_MENU.toString()),
                    new UserButton("Изменить меню", CallbackState.CHANGE_MENU.toString())),
                markup);
            sendMessageWithKeyboard(
                user,
                String.format(MENU_MESSAGE, user.getCity().getValue()),
                markup,
                getMessageId(update),
                sender);
          }
        } else {
          sendMessage(user, MENU_NOT_EXIST, getMessageId(update), sender);
        }
      }
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";

  private static final String MENU_NOT_EXIST =
      "Меню не создано, сначала создайте меню. Чтобы вернуться нажмите \return";

  private static final String MENU_READY_MESSAGE =
      "Вот меню для города *%s*. Оно уже опубликовано. \n" + "Чтобы отменить нажми /cancel";

  private static final String MENU_MESSAGE =
      "Вот меню для города *%s*.\n" + "Чтобы отменить нажми /cancel";
}
