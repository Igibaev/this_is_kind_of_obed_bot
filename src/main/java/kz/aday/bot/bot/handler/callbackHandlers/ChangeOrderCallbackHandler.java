/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ChangeOrderCallbackHandler extends AbstractHandler implements CallbackHandler {
  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      Menu menu = menuService.findById(user.getCity().toString());
      Order order = orderService.findById(user.getId());
      if (menu.isDeadlinePassed()) {
        sendMessage(user, MENU_DEADLINE_IS_PASSED, getMessageId(callback), sender);
      }
      order.setStatus(Status.PENDING);
      orderService.save(order);
      sendMessageWithKeyboard(
          user,
          CHOOSE_ITEM_MESSAGE + "\n\n" + menu.getMenuAsFormattedText(),
          KeyboardUtil.createInlineKeyboard(
              menu.getItemList(), order.getOrderItemList(), CallbackState.ADD_ITEM_TO_ORDER),
          getMessageId(callback),
          sender);
    }
  }

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.CHANGE_ORDER);
  }

  private static final String MENU_DEADLINE_IS_PASSED = "Дедлайн уже прошел.";

  private static final String CHOOSE_ITEM_MESSAGE = "Выберите что хотите заказать:";
}
