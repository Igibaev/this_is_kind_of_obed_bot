/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetOrderTodayAlmataCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.GET_ORDER_TODAY_ALMATA);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      // "Заказ на сегодня" = что едим сегодня = заказ сделанный вчера
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Optional<Order> orderOpt = orderService.findByIdOnDate(user.getId(), yesterday);
      if (orderOpt.isPresent() && !orderOpt.get().getOrderItemList().isEmpty()) {
        sendMessage(
            user,
            Messages.YOUR_ORDER_IS_TODAY.getText(orderOpt.get().getOrderItemList()),
            getMessageId(callback),
            sender);
      } else {
        sendMessage(user, Messages.ORDER_IS_EMPTY_TODAY.getText(), getMessageId(callback), sender);
      }
    }
  }
}
