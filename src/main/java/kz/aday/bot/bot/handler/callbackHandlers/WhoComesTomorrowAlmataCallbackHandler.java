/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import kz.aday.bot.util.OrderCycleDates;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class WhoComesTomorrowAlmataCallbackHandler extends AbstractHandler
    implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.WHO_COMES_TOMORROW_ALMATA);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      LocalDate lunchDate = OrderCycleDates.nextLunchDate(LocalDate.now());
      String lunchDateText = OrderCycleDates.formatLunchDate(lunchDate);
      List<Order> orders =
          orderService.findAllOnDates(OrderCycleDates.orderDatesFor(lunchDate)).stream()
              .filter(o -> o.getCity() == user.getCity())
              .toList();
      String names = orders.stream().map(Order::getUsername).collect(Collectors.joining(", "));
      if (names.isBlank()) {
        sendMessage(
            user,
            Messages.NOBODY_COMES_FOR_LUNCH.getText(lunchDateText),
            getMessageId(callback),
            sender);
      } else {
        sendMessage(
            user,
            Messages.WHO_COMES_FOR_LUNCH.getText(lunchDateText, orders.size(), names),
            getMessageId(callback),
            sender);
      }
    }
  }
}
