/* (C) 2024 Igibaev */
package kz.aday.bot.testsupport;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public final class TestFixtures {

  public static final Long CHAT_ID = 1L;
  public static final String CHAT_ID_STRING = CHAT_ID.toString();
  public static final Integer MESSAGE_ID = 42;

  private TestFixtures() {}

  private static User user(City city, User.Role role, Status status) {
    return User.builder()
        .chatId(CHAT_ID)
        .city(city)
        .role(role)
        .status(status)
        .preferedName(role == User.Role.ADMIN ? "admin" : "me")
        .build();
  }

  public static User adminUser(City city) {
    return user(city, User.Role.ADMIN, Status.READY);
  }

  public static User readyUser() {
    return readyUser(City.ALMATA);
  }

  public static User readyUser(City city) {
    return user(city, User.Role.USER, Status.READY);
  }

  public static User readyUserWithState(State state) {
    User readyUser = readyUser();
    readyUser.setState(state);
    return readyUser;
  }

  public static User userWithStatus(Status status) {
    return user(City.ALMATA, User.Role.USER, status);
  }

  public static User userWithCity(City city) {
    return user(city, User.Role.USER, Status.READY);
  }

  public static User userWithRole(User.Role role) {
    return user(City.ALMATA, role, Status.READY);
  }

  private static Order order(Status status) {
    Order order = new Order();
    order.setChatId(CHAT_ID_STRING);
    order.setStatus(status);
    return order;
  }

  public static Order orderWithStatus(Status status) {
    return order(status);
  }

  public static Order readyOrder() {
    Order readyOrder = order(Status.READY);
    readyOrder.setCity(City.ALMATA);
    readyOrder.setDate(City.ALMATA.getCurrentOrderDate());
    return readyOrder;
  }

  public static Order readyOrderWithItem(City city, String username) {
    Order readyOrder = order(Status.READY);
    readyOrder.setCity(city);
    readyOrder.setUsername(username);
    readyOrder.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    return readyOrder;
  }

  public static Order draftOrder(City city, String username) {
    Order draftOrder = order(Status.PENDING);
    draftOrder.setCity(city);
    draftOrder.setUsername(username);
    draftOrder.setDate(city.getCurrentOrderDate());
    return draftOrder;
  }

  private static Menu menu(City city, Status status) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setStatus(status);
    if (status == Status.READY) {
      menu.setDeadline(LocalDateTime.now().plusHours(1));
    } else if (status == Status.DEADLINE) {
      menu.setDeadline(LocalDateTime.now().minusHours(1));
    }
    return menu;
  }

  public static Menu menuWithStatus(Status status) {
    return menu(City.ALMATA, status);
  }

  public static Menu readyMenu(City city) {
    Menu readyMenu = menu(city, Status.READY);
    readyMenu.setItemList(List.of(new Item(1, "Плов", Category.SECOND)));
    return readyMenu;
  }

  public static String validMenuText() {
    String deadline = LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm"));
    return "Второе\nПлов\n\nДедлайн " + deadline;
  }

  public static Update update() {
    return updateWithChatId(CHAT_ID);
  }

  public static Update updateWithChatId(Long chatId) {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    when(update.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(chatId);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    return update;
  }

  public static Update updateWithText(String text) {
    Update update = update();
    when(update.getMessage().getText()).thenReturn(text);
    return update;
  }

  public static CallbackQuery callbackQuery(String data) {
    CallbackQuery callback = mock(CallbackQuery.class);
    Message message = mock(Message.class);
    when(callback.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(CHAT_ID);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    when(callback.getData()).thenReturn(data);
    return callback;
  }

  public static CallbackQuery callbackQueryWithChatId(Long chatId) {
    CallbackQuery callback = mock(CallbackQuery.class);
    Message message = mock(Message.class);
    when(callback.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(chatId);
    return callback;
  }
}
