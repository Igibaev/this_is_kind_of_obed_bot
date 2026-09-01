/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Report;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetTodayOrdersStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.GET_TODAY_ORDERS.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
      } else if (user.getCity().isNextDayOrderCycle()) {
        List<UserButton> buttons =
            List.of(
                new UserButton(
                    CallbackState.GET_ORDERS_TODAY_ALMATA.getDisplayName(),
                    CallbackState.GET_ORDERS_TODAY_ALMATA.name()),
                new UserButton(
                    CallbackState.GET_ORDERS_TOMORROW_ALMATA.getDisplayName(),
                    CallbackState.GET_ORDERS_TOMORROW_ALMATA.name()));
        sendMessageWithKeyboard(
            user,
            CHOOSE_DATE_MESSAGE,
            KeyboardUtil.createInlineKeyboard(buttons),
            getMessageId(update),
            sender);
      } else {
        List<Order> orders =
            orderService.findAll().stream()
                .filter(o -> o.getCity() == user.getCity())
                .filter(o -> !o.getOrderItemList().isEmpty())
                .filter(o -> o.getStatus() == Status.READY)
                .collect(Collectors.toList());

        if (orders.isEmpty()) {
          sendMessage(user, EMPTY_ORDERS, getMessageId(update), sender);
        } else {
          Report report = new Report(user.getCity(), orders);
          sendMessage(
              user, REPORT_MESSAGE + report.printOrderReport(), getMessageId(update), sender);
        }
      }
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";
  private static final String CHOOSE_DATE_MESSAGE = "Выберите за какой день выгрузить заказы:";
  private static final String EMPTY_ORDERS = "Список заказов пуст.";
  private static final String REPORT_MESSAGE = "Список заказов.\n";
}
