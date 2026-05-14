/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetOrderTodayAlmataCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    String[] data = callback.getData().split(":");
    if (data.length <= 0) {
      throw new IllegalArgumentException("There is no callback");
    }
    return CallbackState.GET_ORDER_TODAY_ALMATA.name().equals(data[0]);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    if (isUserExistAndReady(callback)) {
      User user = userService.findById(getChatId(callback).toString());
      // "Заказ на сегодня" = что едим сегодня = заказ сделанный вчера
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Optional<Order> orderOpt = orderService.findByIdOnDate(user.getId(), yesterday);
      if (orderOpt.isPresent() && !orderOpt.get().getOrderItemList().isEmpty()) {
        sendMessage(
            user,
            String.format(YOUR_ORDER_IS, orderOpt.get().getOrderItemList()),
            getMessageId(callback),
            sender);
      } else {
        sendMessage(user, ORDER_IS_EMPTY, getMessageId(callback), sender);
      }
    }
  }

  private static final String YOUR_ORDER_IS = "Твой заказ на сегодня %s.";
  private static final String ORDER_IS_EMPTY = "Заказ на сегодня не найден.";
}
