/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHamndlers;

import static kz.aday.bot.bot.handler.stateHandlers.State.SET_USERNAME_THEN_CHOOSE_CITY;

import java.time.format.DateTimeFormatter;
import java.util.List;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class StartCommandHandler extends AbstractHandler implements CommandHandler {

  @Override
  public boolean canHandle(String command) {
    return "/start".equalsIgnoreCase(command);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      Menu menu = menuService.findById(user.getCity().toString());
      if (menu != null && menu.getStatus() == Status.READY && !menu.isDeadlinePassed()) {
        if (isOrderExist(user)) {
          Order order = orderService.findById(user.getId());
          if (order.getStatus() == Status.READY) {
            sendMessageWithKeyboard(
                user,
                CURRENT_ORDER,
                getMenuKeyboard(
                    menu.getItemList(),
                    order,
                    List.of(
                        new UserButton("Изменить заказ", CallbackState.CHANGE_ORDER.toString()),
                        new UserButton("Удалить", CallbackState.DELETE_ORDER.toString()),
                        new UserButton("Вернуться", CallbackState.CANCEL.toString()))),
                getMessageId(update),
                sender);
          } else {
            sendMessageWithKeyboard(
                user,
                CURRENT_PENDING_ORDER,
                getMenuKeyboard(
                    menu.getItemList(),
                    order,
                    List.of(
                        new UserButton("Потвердить", CallbackState.SUBMIT_ORDER.toString()),
                        new UserButton("Изменить заказ", CallbackState.CHANGE_ORDER.toString()),
                        new UserButton("Удалить", CallbackState.DELETE_ORDER.toString()),
                        new UserButton("Вернуться", CallbackState.CANCEL.toString()))),
                getMessageId(update),
                sender);
          }
        } else {
          sendMessageWithKeyboard(
              user,
              String.format(MENU_TODAY , user.getCity().getValue(), menu.getDeadline().format(DateTimeFormatter.ISO_TIME)),
              getMenuKeyboard(
                  menu.getItemList(),
                  List.of(new UserButton("Отправить", CallbackState.SUBMIT_ORDER.name()))),
              getMessageId(update),
              sender);
        }
      } else {
        user.setState(State.NONE);
        sendMessageWithKeyboard(
            user, NAVIGATION_MENU, getUserMenuKeyboard(user), getMessageId(update), sender);
      }
    } else {
      User createdUser =
          User.builder()
              .chatId(getChatId(update))
              .role(User.Role.USER)
              .status(Status.PENDING)
              .build();
      createdUser.setState(SET_USERNAME_THEN_CHOOSE_CITY);
      userService.save(createdUser);
      sendMessage(createdUser, START_MESSAGE_INPUT_NAME, getMessageId(update), sender);
    }
  }

  private ReplyKeyboard getMenuKeyboard(
      List<Item> itemList, Order order, List<UserButton> buttons) {
    InlineKeyboardMarkup markup =
        KeyboardUtil.createInlineKeyboard(
            itemList, order.getOrderItemList(), CallbackState.ADD_ITEM_TO_ORDER);
    KeyboardUtil.addButton(buttons, markup);
    return markup;
  }

  private ReplyKeyboard getMenuKeyboard(List<Item> itemList, List<UserButton> buttons) {
    InlineKeyboardMarkup markup =
        KeyboardUtil.createInlineKeyboard(itemList, CallbackState.ADD_ITEM_TO_ORDER);
    KeyboardUtil.addButton(buttons, markup);
    return markup;
  }

  private static final String NAVIGATION_MENU = "Меню навигации по боту.";

  private static final String CURRENT_PENDING_ORDER = "Ты не закончил заказ.";

  private static final String CURRENT_ORDER = "Твой заказ.";

  private static final String MENU_TODAY =
      "Город: *%s*. Вот что сегодня в меню! \n" +
      "Дедлайн до: *%s* \n" +
      "Чтобы отменить заказ нажми /cancel";

  private static final String START_MESSAGE_INPUT_NAME =
      "Добро пожаловать! \n"
          + "Данный бот предназначен \n"
          + "для заказов еды в koronaTech.\n"
          + "Введите своё имя. \n"
          + "Чтобы отменить нажми /cancel";
}
