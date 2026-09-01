/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class CreateOrderStateHandler extends AbstractHandler implements StateHandler {

  @Override
  public boolean canHandle(String state) {
    return State.CREATE_ORDER.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (isMenuExist(user.getCity()) && isMenuReady(user.getCity())) {
        Menu menu = menuService.findById(user.getCity().toString());
        if (menu.isDeadlinePassed()) {
          sendMessage(
              user, Messages.MENU_DEADLINE_IS_PASSED.getText(), getMessageId(update), sender);
        }
        Order order = new Order();
        order.setCity(user.getCity());
        order.setUsername(user.getPreferedName());
        order.setChatId(user.getChatId().toString());
        order.setStatus(Status.PENDING);
        orderService.save(order);
        sendMessageWithKeyboard(
            user,
            Messages.CHOOSE_ITEM + "\n\n" + menu.getMenuAsFormattedText(),
            KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.ADD_ITEM_TO_ORDER),
            getMessageId(update),
            sender);
      } else {
        sendMessage(user, Messages.MENU_IS_NOT_READY_TODAY.getText(), getMessageId(update), sender);
      }
    }
  }
}
