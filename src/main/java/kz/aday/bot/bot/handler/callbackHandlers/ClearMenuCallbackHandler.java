/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ClearMenuCallbackHandler extends AbstractHandler implements CallbackHandler {
  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    if (isUserExistAndReady(callback)) {
      User user = userService.findById(getChatId(callback).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(callback), sender);
        return;
      }
      menuService.deleteById(user.getCity().toString());
      orderService
          .findAll()
          .forEach(
              order -> {
                orderService.deleteById(order.getChatId());
              });
      sendMessage(user, MENU_WAS_DELETED, getMessageId(callback), sender);
    }
  }

  @Override
  public boolean canHandle(CallbackQuery callback) {
    String[] data = callback.getData().split(":");
    if (data.length <= 0) {
      throw new IllegalArgumentException("There is no callback");
    }
    return CallbackState.CLEAR_MENU.toString().equals(data[0]);
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";

  private static final String MENU_WAS_DELETED = "Меню и все заказы были удалены.";
}
