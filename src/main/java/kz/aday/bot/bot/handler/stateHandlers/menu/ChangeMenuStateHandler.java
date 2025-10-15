/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.service.MenuTextParser;
import kz.aday.bot.util.KeyboardUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class ChangeMenuStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.CHANGE_MENU.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
      } else {
        if (user.getState() == State.CHANGE_MENU) {
          LocalDateTime newDeadline = MenuTextParser.parseDeadline(update.getMessage().getText());
          Menu newMenu = MenuTextParser.parseMenu(update.getMessage().getText());
          newMenu.setDeadline(newDeadline);
          newMenu.setStatus(Status.PENDING);
          newMenu.setCity(user.getCity());

          Set<String> users =
              userService.findAll().stream()
                  .filter(u -> u.getCity() == user.getCity())
                  .map(User::getId)
                  .collect(Collectors.toSet());
          List<Order> orders =
              orderService.findAll().stream()
                  .filter(o -> o.getCity() == user.getCity())
                  .filter(o -> users.contains(o.getChatId()))
                  .toList();

          if (orders.isEmpty()) {
            menuService.save(newMenu);
            InlineKeyboardMarkup markup =
                KeyboardUtil.createInlineKeyboard(newMenu.getItemList(), CallbackState.NONE);
            KeyboardUtil.addButton(
                List.of(
                    new UserButton("Опубликовать", CallbackState.SUBMIT_MENU.toString()),
                    new UserButton("Изменить", CallbackState.CHANGE_MENU.toString())),
                markup);
            sendMessageWithKeyboard(
                user,
                String.format(MENU_PENDING, user.getCity().getValue()),
                markup,
                getMessageId(update),
                sender);
          } else {
            log.info("Changing menu when have orders count {}", orders.size());
            Menu oldMenu = menuService.findById(user.getCity().toString());
            List<Item> fromOldMenu =
                oldMenu.getItemList().stream()
                    .filter(item -> newMenu.getItemList().contains(item))
                    .toList();

            menuService.save(newMenu);
            InlineKeyboardMarkup markup =
                KeyboardUtil.createInlineKeyboard(newMenu.getItemList(), CallbackState.NONE);
            KeyboardUtil.addButton(
                List.of(
                    new UserButton("Опубликовать", CallbackState.SUBMIT_MENU.toString()),
                    new UserButton("Изменить", CallbackState.CHANGE_MENU.toString())),
                markup);
            sendMessageWithKeyboard(
                user,
                String.format(MENU_PENDING, user.getCity().getValue()),
                markup,
                getMessageId(update),
                sender);

            if (!fromOldMenu.isEmpty()) {
              log.info("Check if users must be notified");
              for (Order order : orders) {
                log.debug("Order:{} should be changed?", order);
                Iterator<Item> orderItems = order.getOrderItemList().iterator();
                List<Item> bannedItems = new ArrayList<>(order.getOrderItemList().size());
                while (orderItems.hasNext()) {
                  Item item = orderItems.next();
                  if (fromOldMenu.contains(item)) {
                    log.debug("Order:{}. Removed item", item);
                    bannedItems.add(item);
                    orderItems.remove();
                  }
                }
                if (!bannedItems.isEmpty()) {
                  User userHasBannedItems = userService.findById(order.getChatId());
                  log.debug("Send notification to user:{}", userHasBannedItems);
                  sendMessageWithKeyboard(
                      userHasBannedItems,
                      String.format(NOTIFY_IF_USER_HAS_BANNED_ITEMS, bannedItems),
                      markup,
                      getMessageId(update),
                      sender);
                }
              }
            }
          }

        } else {
          user.setState(State.CHANGE_MENU);
          sendMessage(user, MENU_TEMPLATE, getMessageId(update), sender);
        }
      }
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";

  private static final String NOTIFY_IF_USER_HAS_BANNED_ITEMS =
      "Меню изменилось. Из вашего заказа были убраны след позиции %s. "
          + "Выберите то что осталось в меню. "
          + "Или нажмите /cancel чтобы оставить заказ без изменений или удалите его /delete.";

  private static final String MENU_PENDING =
      "Проверьте корректность меню для города %s.\nЧтобы отменить нажми /cancel";

  private static final String MENU_TEMPLATE =
      "Шаблон меню:\n"
          + "Возможны только 5 категории блюд(первое, второе, салат, выпечка, хлеб)\n"
          + "\n"
          + "Первое:\n"
          + "Блюда перечисляются через отступ строки\n"
          + "\n"
          + "Второе:\n"
          + "Блюдо 1\n"
          + "Блюдо 2\n"
          + "\n"
          + "Салат:\n"
          + "1. Блюдо('1. ' это затираться будет)\n"
          + "\n"
          + "Выпечка:\n"
          + "\n"
          + "Хлеб: (если хлеба нету,  либо он всегда к заказу идет, то лучше его убрать)\n"
          + "\n"
          + "Дедлайн 11:00. (дедлайн можно указывать так, можно просто время указывать в формате HH:mm)\n"
          + "\n"
          + "Чтобы отменить нажми /cancel\n";
}
