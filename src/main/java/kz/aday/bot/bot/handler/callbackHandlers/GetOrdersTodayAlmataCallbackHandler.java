/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Report;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import kz.aday.bot.util.OrderCycleDates;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetOrdersTodayAlmataCallbackHandler extends AbstractHandler
    implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.GET_ORDERS_TODAY_ALMATA);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (checkAdminRole(user, getMessageId(callback), sender)) {
        return;
      }
      LocalDate lunchDate = OrderCycleDates.nearestLunchDate(LocalDate.now());
      String lunchDateText = OrderCycleDates.formatLunchDate(lunchDate);
      List<Order> orders =
          orderService.findAllOnDates(OrderCycleDates.orderDatesFor(lunchDate)).stream()
              .filter(o -> o.getCity() == user.getCity())
              .collect(Collectors.toList());

      if (orders.isEmpty()) {
        sendMessage(
            user,
            Messages.EMPTY_ORDERS_FOR_LUNCH.getText(lunchDateText),
            getMessageId(callback),
            sender);
      } else {
        Report report = new Report(user.getCity(), orders);
        sendMessage(
            user,
            Messages.REPORT_ORDERS_FOR_LUNCH.getText(lunchDateText) + report.printOrderReport(),
            getMessageId(callback),
            sender);
      }
    }
  }
}
