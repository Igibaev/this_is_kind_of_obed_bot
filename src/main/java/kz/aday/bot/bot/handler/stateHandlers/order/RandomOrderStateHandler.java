/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Randomizer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class RandomOrderStateHandler extends AbstractHandler implements StateHandler {

  @Override
  public boolean canHandle(String state) {
    return State.RANDOM_ORDER.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (isMenuExist(user.getCity()) && isMenuReady(user.getCity())) {
        if (isDeadLinePassed(user.getCity())) {
          sendMessage(user, MENU_DEADLINE_IS_PASSED, getMessageId(update), sender);
        } else if (user.getState() == State.RANDOM_ORDER) {
          String message = update.getMessage().getText();
          user.setState(State.NONE);
          Menu menu = menuService.findById(user.getCity().toString());
          if (message.equals("Удиви меня") || message.equals("Давай") || message.equals("Ехала")) {
            Order order = orderService.findById(user.getChatId().toString());
            order.setStatus(Status.READY);
            order.setOrderItemList(randomOrder(menu.getItemList()));
            orderService.save(order);
            sendMessage(user, RANDOM_ORDER_CREATED_MESSAGE, getMessageId(update), sender);
          } else {
            sendMessage(user, CANCEL_RANDOM, getMessageId(update), sender);
          }
        } else {
          user.setState(State.RANDOM_ORDER);
          Order order = new Order();
          order.setChatId(user.getChatId());
          order.setStatus(Status.PENDING);
          orderService.save(order);
          ReplyKeyboard keyboard =
              KeyboardUtil.createReplyKeyboard(List.of("Удиви меня", "Нет, я выберу сам"));
          sendMessageWithKeyboard(
              user, RANDOM_ORDER_MESSAGE, keyboard, getMessageId(update), sender);
        }
      }
    }
  }

  private Set<Item> randomOrder(List<Item> itemList) {
    Set<Item> items = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      int randomId = Randomizer.getRandom().nextInt() * itemList.size();
      itemList.stream()
          .filter(item -> item.getId().equals(randomId))
          .findFirst()
          .ifPresent(items::add);
    }
    if (Randomizer.getRandom().nextBoolean()) {
      itemList.stream()
          .filter(item -> item.getCategory().equals(Category.BREAD))
          .findFirst()
          .ifPresent(items::add);
    }
    return items;
  }

  private static final String MENU_DEADLINE_IS_PASSED = "Дедлайн уже прошел.";

  private static final String CANCEL_RANDOM = "Ну ладно, выбери сам. /return";

  private static final String RANDOM_ORDER_MESSAGE = "Вы хотите рандомно сделать заказ?";

  private static final String RANDOM_ORDER_CREATED_MESSAGE =
      "Ваш заказ улетел. Пусть содержимое заказа останется тайной. пока.";
}
