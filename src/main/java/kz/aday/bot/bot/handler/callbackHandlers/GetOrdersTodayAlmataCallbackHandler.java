/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Report;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
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
    if (isUserExistAndReady(callback)) {
      User user = userService.findById(getChatId(callback).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(callback), sender);
        return;
      }
      // "Заказы на сегодня" = кто обедает сегодня = заказы сделанные вчера
      LocalDate yesterday = LocalDate.now().minusDays(1);
      List<Order> orders =
          orderService.findAllOnDate(yesterday).stream()
              .filter(o -> o.getCity() == user.getCity())
              .filter(o -> !o.getOrderItemList().isEmpty())
              .filter(o -> o.getStatus() == Status.READY)
              .collect(Collectors.toList());

      if (orders.isEmpty()) {
        sendMessage(user, EMPTY_ORDERS, getMessageId(callback), sender);
      } else {
        Report report = new Report(user.getCity(), orders);
        sendMessage(
            user, REPORT_MESSAGE + report.printOrderReport(), getMessageId(callback), sender);
      }
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";
  private static final String EMPTY_ORDERS = "Список заказов на сегодня пуст.";
  private static final String REPORT_MESSAGE = "Заказы на сегодня.\n";
}
