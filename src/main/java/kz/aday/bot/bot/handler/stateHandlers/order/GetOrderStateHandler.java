/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.List;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetOrderStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.GET_ORDER.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (user.getCity().isNextDayOrderCycle()) {
        List<UserButton> buttons = List.of(
            new UserButton(CallbackState.GET_ORDER_TODAY_ALMATA.getDisplayName(),
                CallbackState.GET_ORDER_TODAY_ALMATA.name()),
            new UserButton(CallbackState.GET_ORDER_TOMORROW_ALMATA.getDisplayName(),
                CallbackState.GET_ORDER_TOMORROW_ALMATA.name())
        );
        sendMessageWithKeyboard(
            user,
            CHOOSE_DATE_MESSAGE,
            KeyboardUtil.createInlineKeyboard(buttons),
            getMessageId(update),
            sender);
      } else {
        if (isOrderExist(user)) {
          Order order = orderService.findById(user.getId());
          sendMessage(
              user,
              String.format(YOUR_ORDER_IS, order.getOrderItemList()),
              getMessageId(update),
              sender);
        } else {
          sendMessage(user, ORDER_IS_EMPTY, getMessageId(update), sender);
        }
      }
    }
  }

  private static final String CHOOSE_DATE_MESSAGE = "Выберите за какой день посмотреть заказ:";
  private static final String YOUR_ORDER_IS = "Твой заказ %s.";
  private static final String ORDER_IS_EMPTY = "Твой заказ пуст.";
}
