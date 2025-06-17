/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class CreateOrderStateHandler extends AbstractHandler implements StateHandler {

  @Override
  public boolean canHandle(String state) {
    return State.CREATE_ORDER.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (isMenuExist(user.getCity()) && isMenuReady(user.getCity())) {
        Menu menu = menuService.findById(user.getCity().toString());
        if (menu.isDeadlinePassed()) {
          sendMessage(user, MENU_DEADLINE_IS_PASSED, getMessageId(update), sender);
        }
        Order order = new Order();
        order.setUsername(user.getPreferedName());
        order.setChatId(user.getChatId());
        order.setStatus(Status.PENDING);
        orderService.save(order);
        sendMessageWithKeyboard(
            user,
            CHOOSE_ITEM_MESSAGE,
            KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.ADD_ITEM_TO_ORDER),
            getMessageId(update),
            sender);
      }
    }
  }

  private static final String MENU_DEADLINE_IS_PASSED = "Дедлайн уже прошел.";

  private static final String CHOOSE_ITEM_MESSAGE = "Выберите что хотите заказать:";
}
