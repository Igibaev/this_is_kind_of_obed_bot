/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ShareLunchStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.SHARE_LUNCH.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (!isOrderExist(user)) {
        return;
      }
      if (user.getState() == State.SHARE_LUNCH) {
        user.setState(State.NONE);
        String message = update.getMessage().getText();
        if (message.equals("Да")) {
          List<Item> shared = releaseOrderToSharedOrderItemPool(user);
          String text =
              shared.isEmpty()
                  ? Messages.SHARE_LUNCH_NOT_POSSIBLE.getText()
                  : Messages.POOL_ORDER_SHARED.getText(joinItemNames(shared));
          sendMessage(user, text, getMessageId(update), sender);
        } else {
          sendMessage(user, Messages.OK_RETURN_TO_MENU.getText(), getMessageId(update), sender);
        }
      } else {
        Order order = orderService.findByChatId(user.getId(), user.getCity().getCurrentOrderDate());
        ReplyKeyboard keyboard = KeyboardUtil.createReplyKeyboard(List.of("Да", "Нет"));
        user.setState(State.SHARE_LUNCH);
        sendMessageWithKeyboard(
            user,
            Messages.SHARE_LUNCH_CONFIRM.getText(order.getOrderItemList()),
            keyboard,
            getMessageId(update),
            sender);
      }
    }
  }
}
