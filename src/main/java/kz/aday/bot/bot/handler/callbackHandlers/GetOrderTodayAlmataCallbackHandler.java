/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import kz.aday.bot.util.OrderCycleDates;
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
      LocalDate lunchDate = OrderCycleDates.nearestLunchDate(LocalDate.now());
      String lunchDateText = OrderCycleDates.formatLunchDate(lunchDate);
      Optional<Order> orderOpt =
          orderService.findByIdOnDates(user.getId(), OrderCycleDates.orderDatesFor(lunchDate));
      if (orderOpt.isPresent()) {
        sendMessage(
            user,
            Messages.YOUR_ORDER_FOR_LUNCH.getText(lunchDateText, orderOpt.get().getOrderItemList()),
            getMessageId(callback),
            sender);
      } else {
        sendMessage(
            user,
            Messages.ORDER_IS_EMPTY_FOR_LUNCH.getText(lunchDateText),
            getMessageId(callback),
            sender);
      }
    }
  }
}
