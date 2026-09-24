/* (C) 2024 Igibaev */
package kz.aday.bot.bot.flow;

import static kz.aday.bot.testsupport.TestFixtures.callbackQueryWithChatId;
import static kz.aday.bot.testsupport.TestFixtures.updateWithChatId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.RealDispatchers;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class OrderFlowTest extends AbstractDbPersistenceTest {

  private static final Long ADMIN_CHAT_ID = 951000001L;
  private static final Long USER_CHAT_ID = 951000002L;

  @Test
  void newUserOrdersLunch_thenAdminSeesItInReport() throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();

    seedReadyAdmin(ADMIN_CHAT_ID, City.ALMATA);
    seedReadyMenu(City.ALMATA);

    Update start = updateWithChatId(USER_CHAT_ID, "/start");
    dispatchers.commandDispatcher.dispatch(start, sender);

    Update enterName = updateWithChatId(USER_CHAT_ID, "Иван");
    dispatchers.stateDispatcher.dispatch(enterName, sender);

    Update chooseCity = updateWithChatId(USER_CHAT_ID, City.ALMATA.getValue());
    dispatchers.stateDispatcher.dispatch(chooseCity, sender);

    Update createOrder = updateWithChatId(USER_CHAT_ID, State.CREATE_ORDER.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createOrder, sender);

    CallbackQuery addItem =
        callbackQueryWithChatId(USER_CHAT_ID, addItemToOrderCallback(City.ALMATA));
    dispatchers.callbackDispatcher.dispatch(addItem, sender);

    CallbackQuery submitOrder =
        callbackQueryWithChatId(USER_CHAT_ID, CallbackState.SUBMIT_ORDER.name());
    dispatchers.callbackDispatcher.dispatch(submitOrder, sender);

    CallbackQuery adminReport =
        callbackQueryWithChatId(ADMIN_CHAT_ID, CallbackState.GET_ORDERS_TOMORROW.name());
    dispatchers.callbackDispatcher.dispatch(adminReport, sender);

    assertTrue(orderExistsAndReady(USER_CHAT_ID, City.ALMATA.getCurrentOrderDate()));

    Order userOrder =
        ServiceContainer.getOrderService()
            .findByChatId(USER_CHAT_ID.toString(), City.ALMATA.getCurrentOrderDate());
    assertTrue(
        userOrder.getOrderItemList().stream().anyMatch(item -> "Плов".equals(item.getName())),
        "Expected Иван's order to contain Плов, but was: " + userOrder.getOrderItemList());

    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    List<String> messagesToAdmin =
        captor.getAllValues().stream()
            .filter(message -> ADMIN_CHAT_ID.toString().equals(message.getChatId()))
            .map(SendMessage::getText)
            .toList();
    boolean reportSentToAdmin =
        messagesToAdmin.stream()
            .anyMatch(text -> text.contains("Иван") && text.contains("Плов"));
    assertTrue(
        reportSentToAdmin,
        "Expected admin report to list Иван's Плов order. Messages sent to admin: "
            + messagesToAdmin);
  }

  @Test
  void nonAdminUser_cannotSeeOrdersReport_getsPermissionDenied() throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long nonAdminChatId = 951000010L;
    seedReadyUser(nonAdminChatId, City.ALMATA, User.Role.USER);

    CallbackQuery report =
        callbackQueryWithChatId(nonAdminChatId, CallbackState.GET_ORDERS_TODAY.name());
    dispatchers.callbackDispatcher.dispatch(report, sender);

    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean permissionDeniedSent =
        captor.getAllValues().stream()
            .anyMatch(
                message ->
                    nonAdminChatId.toString().equals(message.getChatId())
                        && message.getText().startsWith(Messages.PERMISSION_DENIED.getText()));
    assertTrue(permissionDeniedSent, "Expected non-admin to receive a permission-denied message");
  }

  @Test
  void adminRequestsReport_whenNoOrdersExist_getsEmptyMessage() throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    seedReadyAdmin(ADMIN_CHAT_ID, City.ALMATA);

    CallbackQuery report =
        callbackQueryWithChatId(ADMIN_CHAT_ID, CallbackState.GET_ORDERS_TODAY.name());
    dispatchers.callbackDispatcher.dispatch(report, sender);

    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean emptyMessageSent =
        captor.getAllValues().stream()
            .anyMatch(
                message ->
                    ADMIN_CHAT_ID.toString().equals(message.getChatId())
                        && message.getText().startsWith(Messages.EMPTY_ORDERS_TODAY.getText()));
    assertTrue(emptyMessageSent, "Expected admin to receive an empty-orders-today message");
  }

  @Test
  void selectingSameItemTwice_removesItFromDraftOrder() throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long chatId = 951000011L;
    seedReadyMenu(City.ALMATA, LocalDateTime.now().plusHours(2));

    Update start = updateWithChatId(chatId, "/start");
    dispatchers.commandDispatcher.dispatch(start, sender);
    Update enterName = updateWithChatId(chatId, "Toggle");
    dispatchers.stateDispatcher.dispatch(enterName, sender);
    Update chooseCity = updateWithChatId(chatId, City.ALMATA.getValue());
    dispatchers.stateDispatcher.dispatch(chooseCity, sender);
    Update createOrder = updateWithChatId(chatId, State.CREATE_ORDER.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createOrder, sender);

    CallbackQuery addItem =
        callbackQueryWithChatId(chatId, addItemToOrderCallback(City.ALMATA));
    dispatchers.callbackDispatcher.dispatch(addItem, sender);
    dispatchers.callbackDispatcher.dispatch(addItem, sender);

    Order order =
        ServiceContainer.getOrderService()
            .findByChatId(chatId.toString(), City.ALMATA.getCurrentOrderDate());
    assertTrue(
        order.getOrderItemList().isEmpty(),
        "Selecting the same item twice should toggle it out of the order");
  }

  @Test
  void afterMenuDeadlinePassed_addItemAndSubmitAreBothRejected() throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long chatId = 951000012L;
    seedReadyMenu(City.ALMATA, LocalDateTime.now().minusMinutes(1));

    Update start = updateWithChatId(chatId, "/start");
    dispatchers.commandDispatcher.dispatch(start, sender);
    Update enterName = updateWithChatId(chatId, "Late");
    dispatchers.stateDispatcher.dispatch(enterName, sender);
    Update chooseCity = updateWithChatId(chatId, City.ALMATA.getValue());
    dispatchers.stateDispatcher.dispatch(chooseCity, sender);
    Update createOrder = updateWithChatId(chatId, State.CREATE_ORDER.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createOrder, sender);

    CallbackQuery addItem =
        callbackQueryWithChatId(chatId, addItemToOrderCallback(City.ALMATA));
    dispatchers.callbackDispatcher.dispatch(addItem, sender);

    CallbackQuery submitOrder = callbackQueryWithChatId(chatId, CallbackState.SUBMIT_ORDER.name());
    dispatchers.callbackDispatcher.dispatch(submitOrder, sender);

    Order order =
        ServiceContainer.getOrderService()
            .findByChatId(chatId.toString(), City.ALMATA.getCurrentOrderDate());
    assertTrue(order.getOrderItemList().isEmpty(), "Item should not be added after the deadline");
    assertEquals(
        Status.PENDING, order.getStatus(), "Order should not become READY after the deadline");

    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean deadlinePassedMessageSent =
        captor.getAllValues().stream()
            .anyMatch(
                message ->
                    message.getText().startsWith(Messages.MENU_DEADLINE_IS_PASSED.getText()));
    assertTrue(deadlinePassedMessageSent, "Expected a deadline-passed message to be sent");
  }

  private static String addItemToOrderCallback(City city) {
    Menu menu = ServiceContainer.getMenuService().findById(city.toString());
    return CallbackState.ADD_ITEM_TO_ORDER + ":" + menu.getItemList().get(0).getId();
  }

  private boolean orderExistsAndReady(Long chatId, LocalDate date) {
    return ServiceContainer.getOrderService().findByChatId(chatId.toString(), date).getStatus()
        == Status.READY;
  }

  private static void seedReadyAdmin(Long chatId, City city) {
    seedReadyUser(chatId, city, User.Role.ADMIN);
  }

  private static void seedReadyUser(Long chatId, City city, User.Role role) {
    User user =
        User.builder()
            .chatId(chatId)
            .preferedName(role == User.Role.ADMIN ? "Admin" : "User")
            .city(city)
            .role(role)
            .status(Status.READY)
            .state(State.NONE)
            .build();
    ServiceContainer.getUserService().save(user);
  }

  private static void seedReadyMenu(City city) {
    seedReadyMenu(city, LocalDateTime.now().plusHours(2));
  }

  private static void seedReadyMenu(City city, LocalDateTime deadline) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(1, "Плов", Category.SECOND)));
    menu.setDeadline(deadline);
    menu.setAvailable(true);
    menu.setNotificated(false);
    ServiceContainer.getMenuService().save(menu);
  }

  private static AbsSender mockSender() throws Exception {
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(sender.execute(any(SendMessage.class))).thenReturn(sentMessage);
    return sender;
  }
}
