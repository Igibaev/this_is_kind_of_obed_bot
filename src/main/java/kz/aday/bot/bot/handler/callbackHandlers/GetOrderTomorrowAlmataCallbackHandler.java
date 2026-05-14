/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetOrderTomorrowAlmataCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    String[] data = callback.getData().split(":");
    if (data.length <= 0) {
      throw new IllegalArgumentException("There is no callback");
    }
    return CallbackState.GET_ORDER_TOMORROW_ALMATA.name().equals(data[0]);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    if (isUserExistAndReady(callback)) {
      User user = userService.findById(getChatId(callback).toString());
      // "Заказ на завтра" = что едим завтра = текущий заказ (сделанный сегодня)
      if (isOrderExist(user)) {
        Order order = orderService.findById(user.getId());
        sendMessage(
            user,
            String.format(YOUR_ORDER_IS, order.getOrderItemList()),
            getMessageId(callback),
            sender);
      } else {
        sendMessage(user, ORDER_IS_EMPTY, getMessageId(callback), sender);
      }
    }
  }

  private static final String YOUR_ORDER_IS = "Твой заказ на завтра %s.";
  private static final String ORDER_IS_EMPTY = "Заказ на завтра не найден.";
}
