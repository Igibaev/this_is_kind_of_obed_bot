/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
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
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (isMenuExist(user.getCity()) && isMenuReady(user.getCity())) {
        Order order = orderService.findById(user.getId());
        Menu menu = menuService.findById(user.getCity().toString());
        if (menu.isDeadlinePassed()) {
          sendMessage(
              user,
              Messages.MENU_DEADLINE_IS_PASSED_ORDER_SENT.getText(),
              getMessageId(update),
              sender);
        } else {
          if (user.getState() == State.DELETE_ORDER) {
            user.setState(State.NONE);
            String message = update.getMessage().getText();
            if (message.equals("Да")) {
              orderService.deleteById(order.getId());
              sendMessage(user, Messages.ORDER_WAS_DELETED.getText(), getMessageId(update), sender);
            } else {
              sendMessage(user, Messages.OK_RETURN_TO_MENU.getText(), getMessageId(update), sender);
            }
          } else {
            ReplyKeyboard keyboard = KeyboardUtil.createReplyKeyboard(List.of("Да", "Нет"));
            user.setState(State.DELETE_ORDER);
            sendMessageWithKeyboard(
                user,
                Messages.YOUR_ORDER_IS_DELETE_CONFIRM.getText(order.getOrderItemList()),
                keyboard,
                getMessageId(update),
                sender);
          }
        }
      }
    }
  }
}
