/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class DeleteOrderCallbackHandler extends AbstractHandler implements CallbackHandler {
  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    if (isUserExistAndReady(callback)) {
      User user = userService.findById(getChatId(callback).toString());
      if (isOrderExist(user)) {
        orderService.deleteById(user.getId());
        sendMessage(user, ORDER_DELETED, getMessageId(callback), sender);
      }
    }
  }

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.DELETE_ORDER);
  }

  private static final String ORDER_DELETED = "Ваш заказ удален./return";
}
