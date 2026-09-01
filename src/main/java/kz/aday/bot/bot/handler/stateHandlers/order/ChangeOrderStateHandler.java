/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ChangeOrderStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.CHANGE_ORDER.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (isMenuExist(user.getCity()) && isMenuReady(user.getCity()) && isOrderExist(user)) {
        Order order = orderService.findById(user.getId());
        Menu menu = menuService.findById(user.getCity().toString());
        if (menu.isDeadlinePassed()) {
          sendMessage(
              user, Messages.MENU_DEADLINE_IS_PASSED.getText(), getMessageId(update), sender);
        } else {
          ReplyKeyboard keyboard =
              KeyboardUtil.createInlineKeyboard(
                  List.of(
                      new UserButton("Изменить заказ", CallbackState.CHANGE_ORDER.toString()),
                      new UserButton("Удалить заказ", CallbackState.DELETE_ORDER.toString()),
                      new UserButton("Отмена", CallbackState.CANCEL.toString())));
          sendMessageWithKeyboard(
              user,
              Messages.YOUR_ORDER_IS.getText(order.getOrderItemList()),
              keyboard,
              getMessageId(update),
              sender);
        }
      }
    }
  }
}
