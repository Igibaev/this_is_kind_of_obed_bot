/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.List;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class DeleteOrderStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.DELETE_ORDER.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (isMenuExist(user.getCity()) && isMenuReady(user.getCity())) {
        Order order = orderService.findById(user.getId());
        Menu menu = menuService.findById(user.getCity().toString());
        if (menu.isDeadlinePassed()) {
          sendMessage(user, MENU_DEADLINE_IS_PASSED, getMessageId(update), sender);
        } else {
          if (user.getState() == State.DELETE_ORDER) {
            user.setState(State.NONE);
            String message = update.getMessage().getText();
            if (message.equals("Да")) {
              orderService.deleteById(order.getId());
              sendMessage(user, ORDER_WAS_DELETED, getMessageId(update), sender);
            } else {
              sendMessage(user, RETURN_TO_MENU, getMessageId(update), sender);
            }
          } else {
            ReplyKeyboard keyboard = KeyboardUtil.createReplyKeyboard(List.of("Да", "Нет"));
            user.setState(State.DELETE_ORDER);
            sendMessageWithKeyboard(
                user,
                String.format(YOUR_ORDER_IS, order.getOrderItemList()),
                keyboard,
                getMessageId(update),
                sender);
          }
        }
      }
    }
  }

  private static final String RETURN_TO_MENU = "Окей. Вернитесь в меню тогда /return";

  private static final String ORDER_WAS_DELETED = "Ваш заказ удален.";

  private static final String YOUR_ORDER_IS = "Твой заказ %s. Чтобы удалить отправьте 'Да'.";

  private static final String MENU_DEADLINE_IS_PASSED = "Дедлайн уже прошел, заказ отправлен.";
}
