/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import java.util.List;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ClearMenuStateHandler extends AbstractHandler implements StateHandler {

  @Override
  public boolean canHandle(String state) {
    return State.CLEAR_MENU.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
      } else {
        if (isMenuExist(user.getCity())) {
          sendMessageWithKeyboard(
              user,
              MENU_TO_DELETE,
              KeyboardUtil.createInlineKeyboard(
                  List.of(
                      new UserButton("Удалить меню", CallbackState.CLEAR_MENU.toString()),
                      new UserButton("Отменить", CallbackState.CANCEL.toString()))),
              getMessageId(update),
              sender);
        } else {
          sendMessage(user, MENU_NOT_EXIST, getMessageId(update), sender);
        }
      }
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";

  private static final String MENU_TO_DELETE = "Вы хотите удалить меню на сегодня?\n";

  private static final String MENU_NOT_EXIST =
      "Меню не создано для города *%s*\n" + "Нажмите /return чтобы вернутся в меню.\n";
}
