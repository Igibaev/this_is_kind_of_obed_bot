/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class WhoComesTodayAlmataCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.WHO_COMES_TODAY_ALMATA);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      // Кто приходит сегодня = заказы сделанные вчера
      LocalDate yesterday = LocalDate.now().minusDays(1);
      List<Order> orders =
          orderService.findAllOnDate(yesterday).stream()
              .filter(o -> o.getCity() == user.getCity())
              .filter(o -> o.getStatus() == Status.READY)
              .toList();
      String names = orders.stream().map(Order::getUsername).collect(Collectors.joining(", "));
      if (names.isBlank()) {
        sendMessage(user, Messages.NOBODY_COMES_TODAY.getText(), getMessageId(callback), sender);
      } else {
        sendMessage(
            user,
            Messages.WHO_COMES_TODAY.getText(orders.size(), names),
            getMessageId(callback),
            sender);
      }
    }
  }
}
