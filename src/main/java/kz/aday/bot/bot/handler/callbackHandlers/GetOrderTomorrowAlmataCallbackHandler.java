/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetOrderTomorrowAlmataCallbackHandler extends AbstractHandler
    implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.GET_ORDER_TOMORROW_ALMATA);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      // "Заказ на завтра" = что едим завтра = текущий заказ (сделанный сегодня)
      if (isOrderExist(user)) {
        Order order = orderService.findById(user.getId());
        sendMessage(
            user,
            Messages.YOUR_ORDER_IS_TOMORROW.getText(order.getOrderItemList()),
            getMessageId(callback),
            sender);
      } else {
        sendMessage(
            user, Messages.ORDER_IS_EMPTY_TOMORROW.getText(), getMessageId(callback), sender);
      }
    }
  }
}
