/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.List;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Report;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetAllOrdersStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.GET_ALL_ORDERS.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
      } else {
        List<Order> orders =
            orderService.findAll().stream()
                .filter(o -> o.getStatus() == Status.READY)
                .collect(Collectors.toList());

        if (orders.isEmpty()) {
          sendMessage(user, EMPTY_ORDERS, getMessageId(update), sender);
        } else {
          Report report = new Report(user.getCity(), orders);
          reportService.save(report);
          sendMessage(
              user, REPORT_MESSAGE + report.printOrderReport(), getMessageId(update), sender);
        }
      }
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";

  private static final String EMPTY_ORDERS = "Список заказов пуст.";

  private static final String REPORT_MESSAGE = "Список заказов.\n";
}
