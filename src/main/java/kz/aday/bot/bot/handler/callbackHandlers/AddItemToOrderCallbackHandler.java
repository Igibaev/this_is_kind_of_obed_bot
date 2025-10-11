/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.List;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.service.MenuRulesService;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class AddItemToOrderCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    String[] data = callback.getData().split(":");
    if (data.length <= 0) {
      throw new IllegalArgumentException("There is no callback");
    }
    return CallbackState.ADD_ITEM_TO_ORDER.toString().equals(data[0].trim());
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    if (isUserExistAndReady(callback)) {
      User user = userService.findById(getChatId(callback).toString());
      Menu menu = menuService.findById(user.getCity().toString());
      Order order;
      if (isOrderExist(user)) {
        order = orderService.findById(user.getId());
      } else {
        order = new Order();
        order.setUsername(user.getPreferedName());
        order.setStatus(Status.PENDING);
        order.setChatId(user.getChatId().toString());
      }

      if (menu.isDeadlinePassed()) {
        sendMessage(user, MENU_DEADLINE_IS_PASSED, getMessageId(callback), sender);
      } else {
        menu.getItemById(getItemId(callback))
            .ifPresent(
                item ->
                    orderService.addItemToOrder(
                        order, item, MenuRulesService.getMenuRule(user.getCity())));
        InlineKeyboardMarkup keyboard =
            KeyboardUtil.createInlineKeyboard(
                menu.getItemList(), order.getOrderItemList(), CallbackState.ADD_ITEM_TO_ORDER);
        KeyboardUtil.addButton(
            List.of(
                new UserButton("Потвердить", CallbackState.SUBMIT_ORDER.toString()),
                new UserButton("Отменить", CallbackState.CANCEL.toString())),
            keyboard);
        sendMessageWithKeyboard(
            user, CREATING_ORDER_MESSAGE, keyboard, getMessageId(callback), sender);
        orderService.save(order);
      }
    }
  }

  private Integer getItemId(CallbackQuery callback) {
    return Integer.parseInt(callback.getData().split(":")[1]);
  }

  private static final String CREATING_ORDER_MESSAGE = "Собираем ваш заказ.";

  private static final String MENU_DEADLINE_IS_PASSED = "Дедлайн уже прошел.";
}
