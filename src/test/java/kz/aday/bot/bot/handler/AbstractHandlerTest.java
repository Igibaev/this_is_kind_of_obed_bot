/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID;
import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.MESSAGE_ID;
import static kz.aday.bot.testsupport.TestFixtures.callbackQueryWithChatId;
import static kz.aday.bot.testsupport.TestFixtures.menuWithStatus;
import static kz.aday.bot.testsupport.TestFixtures.orderWithStatus;
import static kz.aday.bot.testsupport.TestFixtures.updateWithChatId;
import static kz.aday.bot.testsupport.TestFixtures.userWithCity;
import static kz.aday.bot.testsupport.TestFixtures.userWithRole;
import static kz.aday.bot.testsupport.TestFixtures.userWithStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

class AbstractHandlerTest {

  private static final String CITY_KEY = City.ALMATA.toString();
  private static final String MESSAGE_TEXT = "text";
  private static final Long OTHER_CHAT_ID = 10L;
  private static final String OTHER_CHAT_ID_STRING = OTHER_CHAT_ID.toString();
  private static final Integer SENT_MESSAGE_ID = 999;
  private static final Integer PREVIOUS_SENT_MESSAGE_ID = 5;
  private static final Integer PREVIOUS_USER_MESSAGE_ID = 6;

  private static final List<String> USER_BASE_ITEMS =
      List.of(
          State.PROFILE_MENU.getDisplayName(),
          State.MENU_CATEGORY_ORDER.getDisplayName(),
          State.STATISTICS_MENU.getDisplayName(),
          State.WHO_WILL_COME_TO_OFFICE.getDisplayName(),
          State.SET_OFFICE_ATTENDANCE.getDisplayName());

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private OrderService orderService;
  private MessageSender messageSender;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private AbstractHandler handler;

  @BeforeEach
  void setUp() {
    userService = services.getUserService();
    messageSender = services.getMessageService();
    menuService = services.getMenuService();
    orderService = services.getOrderService();
    sharedOrderItemPoolService = services.getPoolService();
    handler = new AbstractHandler() {};
  }

  @Test
  void getChatId_givenUpdate_whenCalled_thenReturnsChatIdFromMessage() {
    // given
    Update update = updateWithChatId(CHAT_ID);
    // when
    Long actual = handler.getChatId(update);
    // then
    assertEquals(CHAT_ID, actual);
  }

  @Test
  void getChatId_givenCallbackQuery_whenCalled_thenReturnsChatIdFromMessage() {
    // given
    CallbackQuery callbackQuery = callbackQueryWithChatId(CHAT_ID);
    // when
    Long actual = handler.getChatId(callbackQuery);
    // then
    assertEquals(CHAT_ID, actual);
  }

  @Test
  void getMessageId_givenUpdate_whenCalled_thenReturnsMessageId() {
    // given
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    when(update.getMessage()).thenReturn(message);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    // when
    Integer actual = handler.getMessageId(update);
    // then
    assertEquals(MESSAGE_ID, actual);
  }

  @Test
  void getMessageId_givenCallbackQuery_whenCalled_thenReturnsMessageId() {
    // given
    CallbackQuery callbackQuery = mock(CallbackQuery.class);
    Message message = mock(Message.class);
    when(callbackQuery.getMessage()).thenReturn(message);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    // when
    Integer actual = handler.getMessageId(callbackQuery);
    // then
    assertEquals(MESSAGE_ID, actual);
  }

  @ParameterizedTest(name = "status={0} -> ready={1}")
  @MethodSource("userStatusToReady")
  void isUserReady_givenUserStatus_whenCalled_thenMatchesReadyExpectation(
      Status status, boolean expected) {
    // given
    User user = userWithStatus(status);
    // when
    boolean actual = handler.isUserReady(user);
    // then
    assertEquals(expected, actual);
  }

  static Stream<Arguments> userStatusToReady() {
    return Stream.of(
        Arguments.of(Status.READY, true),
        Arguments.of(Status.PENDING, false),
        Arguments.of(Status.DEADLINE, false),
        Arguments.of(Status.DELETED, false),
        Arguments.of(Status.OVER_CHANGING, false));
  }

  @ParameterizedTest(name = "userPresent={0}")
  @MethodSource("booleanCases")
  void isUserExist_givenUserPresenceInRepository_whenCalled_thenReflectsPresence(
      boolean userPresent) {
    // given
    Update update = updateWithChatId(CHAT_ID);
    when(userService.findByIdOptional(CHAT_ID_STRING))
        .thenReturn(userPresent ? Optional.of(userWithStatus(Status.READY)) : Optional.empty());
    // when
    boolean actual = handler.isUserExist(update);
    // then
    assertEquals(userPresent, actual);
  }

  static Stream<Boolean> booleanCases() {
    return Stream.of(true, false);
  }

  @ParameterizedTest(name = "userPresent={0}, status={1} -> {2}")
  @MethodSource("userPresenceAndStatusCases")
  void isUserExistAndReady_givenUpdate_whenCalled_thenTrueOnlyWhenPresentAndReady(
      boolean userPresent, Status status, boolean expected) {
    // given
    Update update = updateWithChatId(CHAT_ID);
    stubUser(userPresent, status);
    // when
    boolean actual = handler.isUserExistAndReady(update);
    // then
    assertEquals(expected, actual);
  }

  @ParameterizedTest(name = "userPresent={0}, status={1} -> {2}")
  @MethodSource("userPresenceAndStatusCases")
  void isUserExistAndReady_givenCallbackQuery_whenCalled_thenTrueOnlyWhenPresentAndReady(
      boolean userPresent, Status status, boolean expected) {
    // given
    CallbackQuery callbackQuery = callbackQueryWithChatId(CHAT_ID);
    stubUser(userPresent, status);
    // when
    boolean actual = handler.isUserExistAndReady(callbackQuery);
    // then
    assertEquals(expected, actual);
  }

  static Stream<Arguments> userPresenceAndStatusCases() {
    return Stream.of(
        Arguments.of(false, Status.READY, false),
        Arguments.of(true, Status.READY, true),
        Arguments.of(true, Status.PENDING, false));
  }

  @ParameterizedTest(name = "userPresent={0}, status={1} -> {2}")
  @MethodSource("userPresenceAndStatusCases")
  void findReadyUserByChatId_givenUpdate_whenCalled_thenPresentOnlyWhenReady(
      boolean userPresent, Status status, boolean expectedPresent) {
    // given
    Update update = updateWithChatId(CHAT_ID);
    User user = stubUser(userPresent, status);
    // when
    Optional<User> actual = handler.findReadyUserByChatId(update);
    // then
    assertEquals(expectedPresent, actual.isPresent());
    actual.ifPresent(found -> assertEquals(user, found));
  }

  @ParameterizedTest(name = "userPresent={0}, status={1} -> {2}")
  @MethodSource("userPresenceAndStatusCases")
  void findReadyUserByChatId_givenCallbackQuery_whenCalled_thenPresentOnlyWhenReady(
      boolean userPresent, Status status, boolean expectedPresent) {
    // given
    CallbackQuery callbackQuery = callbackQueryWithChatId(CHAT_ID);
    User user = stubUser(userPresent, status);
    // when
    Optional<User> actual = handler.findReadyUserByChatId(callbackQuery);
    // then
    assertEquals(expectedPresent, actual.isPresent());
    actual.ifPresent(found -> assertEquals(user, found));
  }

  @ParameterizedTest(name = "menuPresent={0}")
  @MethodSource("booleanCases")
  void isMenuExist_givenMenuPresenceInRepository_whenCalled_thenReflectsPresence(
      boolean menuPresent) {
    // given
    when(menuService.existsById(CITY_KEY)).thenReturn(menuPresent);
    // when
    boolean actual = handler.isMenuExist(City.ALMATA);
    // then
    assertEquals(menuPresent, actual);
  }

  @ParameterizedTest(name = "menuStatus={0} -> ready={1}")
  @MethodSource("menuStatusToReady")
  void isMenuReady_givenMenuStatus_whenCalled_thenTrueOnlyWhenReady(
      Status menuStatus, boolean expected) {
    // given
    stubMenu(menuStatus);
    // when
    boolean actual = handler.isMenuReady(City.ALMATA);
    // then
    assertEquals(expected, actual);
  }

  static Stream<Arguments> menuStatusToReady() {
    return Stream.of(
        Arguments.of((Object) null, false),
        Arguments.of(Status.READY, true),
        Arguments.of(Status.PENDING, false),
        Arguments.of(Status.DEADLINE, false));
  }

  @Test
  void isDeadLinePassed_givenNoMenu_whenCalled_thenReturnsFalse() {
    // given
    stubMenu(null);
    // when
    boolean actual = handler.isDeadLinePassed(City.ALMATA);
    // then
    assertFalse(actual);
  }

  @Test
  void isDeadLinePassed_givenMenuWithPastDeadline_whenCalled_thenReturnsTrue() {
    // given
    stubMenuWithDeadline(LocalDateTime.now().minusMinutes(1));
    // when
    boolean actual = handler.isDeadLinePassed(City.ALMATA);
    // then
    assertTrue(actual);
  }

  @Test
  void isDeadLinePassed_givenMenuWithFutureDeadline_whenCalled_thenReturnsFalse() {
    // given
    stubMenuWithDeadline(LocalDateTime.now().plusMinutes(1));
    // when
    boolean actual = handler.isDeadLinePassed(City.ALMATA);
    // then
    assertFalse(actual);
  }

  @ParameterizedTest(name = "orderPresent={0}")
  @MethodSource("booleanCases")
  void isOrderExist_givenOrderPresenceInRepository_whenCalled_thenReflectsPresence(
      boolean orderPresent) {
    // given
    User user = userWithStatus(Status.READY);
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(orderPresent);
    // when
    boolean actual = handler.isOrderExist(user);
    // then
    assertEquals(orderPresent, actual);
  }

  @Test
  void releaseOrderToPool_returnsEmptyList_whenNoOrderSharedOrderItem() {
    // given
    User user = userWithStatus(Status.READY);
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(false);
    // when
    List<Item> actual = handler.releaseOrderToSharedOrderItemPool(user);
    // then
    assertEquals(List.of(), actual);
    verify(orderService, never()).deleteByChatId(any(), any());
    verify(sharedOrderItemPoolService, never())
        .addItems(any(), any(), any(), any(), anyCollection());
  }

  @Test
  void releaseOrderToPool_returnsEmptyListAndKeepsOrder_whenOrderHasNoItems() {
    // given
    User user = userWithStatus(Status.READY);
    LocalDate orderDate = City.ALMATA.getCurrentOrderDate();
    Order order = orderWithStatus(Status.READY);
    order.setDate(orderDate);
    when(orderService.existsByChatId(CHAT_ID_STRING, orderDate)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, orderDate)).thenReturn(order);
    // when
    List<Item> actual = handler.releaseOrderToSharedOrderItemPool(user);
    // then
    assertEquals(List.of(), actual);
    verify(orderService, never()).deleteByChatId(CHAT_ID_STRING, orderDate);
    verify(sharedOrderItemPoolService, never())
        .addItems(any(), any(), any(), any(), anyCollection());
  }

  @Test
  void releaseOrderToPool_movesItemsToPoolAndDeletesOrder_whenOrderHasItemsAndDeadlinePassed() {
    // given
    User user = userWithStatus(Status.READY);
    LocalDate orderDate = City.ALMATA.getCurrentOrderDate();
    Item item = new Item(1, "Плов", null);
    Order order = orderWithStatus(Status.READY);
    order.setDate(orderDate);
    order.getOrderItemList().add(item);
    when(orderService.existsByChatId(CHAT_ID_STRING, orderDate)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, orderDate)).thenReturn(order);
    order.setSubmittedAt(LocalDateTime.now().minusMinutes(1));
    // when
    List<Item> actual = handler.releaseOrderToSharedOrderItemPool(user);
    // then
    assertEquals(List.of(item), actual);
    verify(orderService).deleteByChatId(CHAT_ID_STRING, orderDate);
    verify(sharedOrderItemPoolService)
        .addItems(
            user.getCity(), order.getDate(), user.getId(), user.getPreferedName(), Set.of(item));
  }

  @Test
  void releaseOrderToPool_keepsOrderWithoutSharing_whenNotYetSubmittedToVendor() {
    // given
    User user = userWithStatus(Status.READY);
    LocalDate orderDate = City.ALMATA.getCurrentOrderDate();
    Item item = new Item(1, "Плов", null);
    Order order = orderWithStatus(Status.READY);
    order.setDate(orderDate);
    order.getOrderItemList().add(item);
    when(orderService.existsByChatId(CHAT_ID_STRING, orderDate)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, orderDate)).thenReturn(order);
    // submittedAt stays null: order not yet submitted to vendor
    // when
    List<Item> actual = handler.releaseOrderToSharedOrderItemPool(user);
    // then
    assertEquals(List.of(), actual);
    verify(orderService, never()).deleteByChatId(CHAT_ID_STRING, orderDate);
    verify(sharedOrderItemPoolService, never())
        .addItems(any(), any(), any(), any(), anyCollection());
  }

  @Test
  void releaseOrderToPool_usesOrdersOwnStoredDate_regardlessOfCurrentCityCycle() {
    // given
    User user = userWithCity(City.ALMATA);
    LocalDate orderDate = City.ALMATA.getCurrentOrderDate();
    Item item = new Item(1, "Плов", null);
    Order order = orderWithStatus(Status.READY);
    LocalDate storedDate = LocalDate.now().plusDays(5);
    order.setDate(storedDate);
    order.getOrderItemList().add(item);
    when(orderService.existsByChatId(CHAT_ID_STRING, orderDate)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, orderDate)).thenReturn(order);
    order.setSubmittedAt(LocalDateTime.now().minusMinutes(1));
    // when
    handler.releaseOrderToSharedOrderItemPool(user);
    // then
    verify(sharedOrderItemPoolService)
        .addItems(eq(City.ALMATA), eq(storedDate), any(), any(), anyCollection());
  }

  @Test
  void releaseOrderToPool_fallsBackToCityCurrentOrderDate_whenOrderHasNoStoredDate() {
    // given
    User user = userWithCity(City.ALMATA);
    LocalDate orderDate = City.ALMATA.getCurrentOrderDate();
    Item item = new Item(1, "Плов", null);
    Order order = orderWithStatus(Status.READY);
    order.setDate(null);
    order.getOrderItemList().add(item);
    when(orderService.existsByChatId(CHAT_ID_STRING, orderDate)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, orderDate)).thenReturn(order);
    order.setSubmittedAt(LocalDateTime.now().minusMinutes(1));
    // when
    handler.releaseOrderToSharedOrderItemPool(user);
    // then
    verify(sharedOrderItemPoolService)
        .addItems(
            eq(City.ALMATA), eq(City.ALMATA.getCurrentOrderDate()), any(), any(), anyCollection());
  }

  @Test
  void joinItemNames_returnsCommaSeparatedNames_whenCalled() {
    // given
    Item first = new Item(1, "Плов", null);
    Item second = new Item(2, "Лагман", null);
    // when
    String actual = handler.joinItemNames(List.of(first, second));
    // then
    assertEquals("Плов, Лагман", actual);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("orderMenuItemsCases")
  void getOrderMenuItems_givenMenuAndOrderState_whenCalled_thenBuildsExpectedItems(
      MenuKeyboardCase testCase) {
    // given
    User user = userWithRole(User.Role.USER);
    stubMenu(testCase.menuStatus());
    if (testCase.menuStatus() == Status.READY) {
      when(orderService.findByChatIdOptional(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
          .thenReturn(
              testCase.orderStatus() == null
                  ? Optional.empty()
                  : Optional.of(orderWithStatus(testCase.orderStatus())));
    }
    if (testCase.menuStatus() == Status.DEADLINE) {
      when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
          .thenReturn(testCase.orderStatus() != null);
    }
    // when
    List<String> actual = handler.getOrderMenuItems(user);
    // then
    List<String> expected =
        concat(List.of(State.VIEW_POOL.getDisplayName()), testCase.extraItems());
    assertEquals(expected, actual);
  }

  @Test
  void
      getOrderMenuItems_givenPendingMenuWithLeftoverOrderFromPreviousCycle_whenCalled_thenStillShowsGetOrder() {
    // given
    User user = userWithRole(User.Role.USER);
    stubMenu(Status.PENDING);
    when(orderService.existsByChatId(CHAT_ID_STRING, LocalDate.now())).thenReturn(true);
    // when
    List<String> actual = handler.getOrderMenuItems(user);
    // then
    assertEquals(
        List.of(State.VIEW_POOL.getDisplayName(), State.GET_ORDER.getDisplayName()), actual);
  }

  @Test
  void
      getOrderMenuItems_givenReadyMenuWithNoCurrentOrderButLeftoverOrder_whenCalled_thenStillShowsGetOrder() {
    // given
    User user = userWithRole(User.Role.USER);
    stubMenu(Status.READY);
    when(orderService.findByChatIdOptional(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(Optional.empty());
    when(orderService.existsByChatId(CHAT_ID_STRING, LocalDate.now())).thenReturn(true);
    // when
    List<String> actual = handler.getOrderMenuItems(user);
    // then
    assertEquals(
        List.of(
            State.VIEW_POOL.getDisplayName(),
            State.VIEW_MENU_TODAY.getDisplayName(),
            State.CREATE_ORDER.getDisplayName(),
            State.RANDOM_ORDER.getDisplayName(),
            State.GET_ORDER.getDisplayName()),
        actual);
  }

  @Test
  void
      getOrderMenuItems_givenNoMenuButLeftoverOrderFromClearedCycle_whenCalled_thenStillShowsGetOrder() {
    // given
    User user = userWithRole(User.Role.USER);
    stubMenu(null);
    when(orderService.existsByChatId(CHAT_ID_STRING, LocalDate.now())).thenReturn(true);
    // when
    List<String> actual = handler.getOrderMenuItems(user);
    // then
    assertEquals(
        List.of(
            State.VIEW_POOL.getDisplayName(),
            State.CREATE_ORDER.getDisplayName(),
            State.GET_ORDER.getDisplayName()),
        actual);
  }

  @Test
  void getUserMenuKeyboard_givenUser_whenCalled_thenAlwaysShowsOrderMenuCategoryButton() {
    // given
    User user = userWithRole(User.Role.USER);
    stubMenu(null);
    // when
    ReplyKeyboard actual = handler.getUserMenuKeyboard(user);
    // then
    assertEquals(USER_BASE_ITEMS, buttonTexts(actual));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("adminMenuCrudCases")
  void getUserMenuKeyboard_givenAdmin_whenCalled_thenShowsMenuCrudItemsForMenuState(
      MenuKeyboardCase testCase) {
    // given
    User user = userWithRole(User.Role.ADMIN);
    stubMenu(testCase.menuStatus());
    // when
    ReplyKeyboard actual = handler.getUserMenuKeyboard(user);
    // then
    assertEquals(adminBaseItems(testCase.extraItems()), buttonTexts(actual));
  }

  record MenuKeyboardCase(
      String name, Status menuStatus, Status orderStatus, List<String> extraItems) {
    @Override
    public String toString() {
      return name;
    }
  }

  static Stream<MenuKeyboardCase> adminMenuCrudCases() {
    return Stream.of(
        new MenuKeyboardCase("no menu", null, null, List.of(State.CREATE_MENU.getDisplayName())),
        new MenuKeyboardCase(
            "ready menu",
            Status.READY,
            null,
            List.of(State.CLEAR_MENU.getDisplayName(), State.CHANGE_MENU.getDisplayName())),
        new MenuKeyboardCase(
            "deadline menu", Status.DEADLINE, null, List.of(State.CHANGE_MENU.getDisplayName())),
        new MenuKeyboardCase(
            "pending menu",
            Status.PENDING,
            null,
            List.of(State.PUBLISH_MENU.getDisplayName(), State.CHANGE_MENU.getDisplayName())),
        new MenuKeyboardCase("deleted menu", Status.DELETED, null, List.of()),
        new MenuKeyboardCase("over-changing menu", Status.OVER_CHANGING, null, List.of()));
  }

  static Stream<MenuKeyboardCase> orderMenuItemsCases() {
    return Stream.of(
        new MenuKeyboardCase("no menu", null, null, List.of(State.CREATE_ORDER.getDisplayName())),
        new MenuKeyboardCase(
            "ready menu, no order",
            Status.READY,
            null,
            List.of(
                State.VIEW_MENU_TODAY.getDisplayName(),
                State.CREATE_ORDER.getDisplayName(),
                State.RANDOM_ORDER.getDisplayName())),
        new MenuKeyboardCase(
            "ready menu, pending order",
            Status.READY,
            Status.PENDING,
            List.of(
                State.VIEW_MENU_TODAY.getDisplayName(),
                State.SUBMIT_ORDER.getDisplayName(),
                State.CHANGE_ORDER.getDisplayName(),
                State.GET_ORDER.getDisplayName())),
        new MenuKeyboardCase(
            "ready menu, ready order",
            Status.READY,
            Status.READY,
            List.of(
                State.VIEW_MENU_TODAY.getDisplayName(),
                State.DELETE_ORDER.getDisplayName(),
                State.CHANGE_ORDER.getDisplayName(),
                State.GET_ORDER.getDisplayName())),
        new MenuKeyboardCase(
            "deadline menu", Status.DEADLINE, null, List.of(State.GET_ORDER.getDisplayName())),
        new MenuKeyboardCase(
            "deadline menu, with order",
            Status.DEADLINE,
            Status.READY,
            List.of(State.GET_ORDER.getDisplayName(), State.SHARE_LUNCH.getDisplayName())),
        new MenuKeyboardCase("pending menu", Status.PENDING, null, List.of()),
        new MenuKeyboardCase("deleted menu", Status.DELETED, null, List.of()),
        new MenuKeyboardCase("over-changing menu", Status.OVER_CHANGING, null, List.of()));
  }

  @ParameterizedTest(name = "lastSentId={0}, userLastMessageId={1} -> deletes={2}")
  @MethodSource("messagesToDeleteCases")
  void sendMessage_givenPreviousMessageIds_whenCalled_thenDeletesThemAndSavesNewOne(
      Integer lastUserSentMessageId, Integer userLastMessageId, List<Integer> expectedToDelete)
      throws TelegramApiException {
    // given
    User user = userWithStatus(Status.READY);
    user.setChatId(OTHER_CHAT_ID);
    user.setLastMessageId(userLastMessageId);
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(SENT_MESSAGE_ID);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);
    // when
    handler.sendMessage(user, MESSAGE_TEXT, lastUserSentMessageId, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    SendMessage actualMessage = messageCaptor.getValue();
    assertEquals(MESSAGE_TEXT, actualMessage.getText());
    assertEquals(OTHER_CHAT_ID_STRING, actualMessage.getChatId());
    ArgumentCaptor<List<Integer>> deleteCaptor = ArgumentCaptor.forClass(List.class);
    verify(messageSender).deleteMessage(eq(OTHER_CHAT_ID), deleteCaptor.capture(), eq(sender));
    List<Integer> actualMessagesToDelete = deleteCaptor.getValue();
    assertEquals(expectedToDelete, actualMessagesToDelete);
    assertEquals(SENT_MESSAGE_ID, user.getLastMessageId());
    verify(userService).save(user);
  }

  static Stream<Arguments> messagesToDeleteCases() {
    return Stream.of(
        Arguments.of(null, null, List.of()),
        Arguments.of(PREVIOUS_SENT_MESSAGE_ID, null, List.of(PREVIOUS_SENT_MESSAGE_ID)),
        Arguments.of(null, PREVIOUS_USER_MESSAGE_ID, List.of(PREVIOUS_USER_MESSAGE_ID)),
        Arguments.of(
            PREVIOUS_SENT_MESSAGE_ID,
            PREVIOUS_USER_MESSAGE_ID,
            List.of(PREVIOUS_SENT_MESSAGE_ID, PREVIOUS_USER_MESSAGE_ID)));
  }

  @ParameterizedTest(name = "lastSentId={0}, userLastMessageId={1} -> deletes={2}")
  @MethodSource("messagesToDeleteCases")
  void sendMessageWithKeyboard_givenPreviousMessageIds_whenCalled_thenDeletesThemAndSavesNewOne(
      Integer lastUserSentMessageId, Integer userLastMessageId, List<Integer> expectedToDelete)
      throws TelegramApiException {
    // given
    User user = userWithStatus(Status.READY);
    user.setChatId(OTHER_CHAT_ID);
    user.setLastMessageId(userLastMessageId);
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(SENT_MESSAGE_ID);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);
    ReplyKeyboard keyboard = new ReplyKeyboardMarkup();
    // when
    handler.sendMessageWithKeyboard(user, MESSAGE_TEXT, keyboard, lastUserSentMessageId, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    SendMessage actualMessage = messageCaptor.getValue();
    assertEquals(MESSAGE_TEXT, actualMessage.getText());
    assertEquals(keyboard, actualMessage.getReplyMarkup());
    ArgumentCaptor<List<Integer>> deleteCaptor = ArgumentCaptor.forClass(List.class);
    verify(messageSender).deleteMessage(eq(OTHER_CHAT_ID), deleteCaptor.capture(), eq(sender));
    List<Integer> actualMessagesToDelete = deleteCaptor.getValue();
    assertEquals(expectedToDelete, actualMessagesToDelete);
    assertEquals(SENT_MESSAGE_ID, user.getLastMessageId());
    verify(userService).save(user);
  }

  @Test
  void sendProfileCard_givenUser_whenCalled_thenSendsProfileInfoAndEditButtons()
      throws TelegramApiException {
    // given
    User user = userWithStatus(Status.READY);
    user.setChatId(OTHER_CHAT_ID);
    user.setPreferedName("Alice");
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(SENT_MESSAGE_ID);
    when(messageSender.sendMessage(any(), eq(sender), eq(true))).thenReturn(sentMessage);
    // when
    handler.sendProfileCard(user, null, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender), eq(true));
    SendMessage actualMessage = messageCaptor.getValue();
    assertEquals(
        Messages.PROFILE_INFO.getText("Alice", City.ALMATA.getValue()), actualMessage.getText());
    assertEquals(
        List.of(
            State.CHANGE_NAME_ONLY.getDisplayName(),
            State.CHANGE_CITY_ONLY.getDisplayName(),
            State.BACK_TO_MENU.getDisplayName()),
        buttonTexts(actualMessage.getReplyMarkup()));
  }

  @Test
  void sendStatisticsCard_givenAdmin_whenCalled_thenSendsAdminAndPersonalStatsButtons()
      throws TelegramApiException {
    // given
    User user = userWithRole(User.Role.ADMIN);
    user.setChatId(OTHER_CHAT_ID);
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(SENT_MESSAGE_ID);
    when(messageSender.sendMessage(any(), eq(sender), eq(true))).thenReturn(sentMessage);
    // when
    handler.sendStatisticsCard(user, null, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender), eq(true));
    assertEquals(
        List.of(
            State.GET_MY_ATTENDANCE_STATS.getDisplayName(),
            State.GET_MY_ATTENDANCE_STATS_MONTH.getDisplayName(),
            State.GET_ATTENDANCE_STATS.getDisplayName(),
            State.GET_ATTENDANCE_STATS_MONTH.getDisplayName(),
            State.BACK_TO_MENU.getDisplayName()),
        buttonTexts(messageCaptor.getValue().getReplyMarkup()));
  }

  @Test
  void sendStatisticsCard_givenUser_whenCalled_thenSendsOnlyPersonalStatsButtons()
      throws TelegramApiException {
    // given
    User user = userWithRole(User.Role.USER);
    user.setChatId(OTHER_CHAT_ID);
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(SENT_MESSAGE_ID);
    when(messageSender.sendMessage(any(), eq(sender), eq(true))).thenReturn(sentMessage);
    // when
    handler.sendStatisticsCard(user, null, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender), eq(true));
    assertEquals(
        List.of(
            State.GET_MY_ATTENDANCE_STATS.getDisplayName(),
            State.GET_MY_ATTENDANCE_STATS_MONTH.getDisplayName(),
            State.BACK_TO_MENU.getDisplayName()),
        buttonTexts(messageCaptor.getValue().getReplyMarkup()));
  }

  @Test
  void sendOrderMenuCategory_givenUser_whenCalled_thenSendsCategoryPromptAndActionButtons()
      throws TelegramApiException {
    // given
    User user = userWithStatus(Status.READY);
    user.setChatId(OTHER_CHAT_ID);
    stubMenu(null);
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(SENT_MESSAGE_ID);
    when(messageSender.sendMessage(any(), eq(sender), eq(true))).thenReturn(sentMessage);
    // when
    handler.sendOrderMenuCategory(user, null, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender), eq(true));
    SendMessage actualMessage = messageCaptor.getValue();
    assertEquals(Messages.CATEGORY_PROMPT.getText(), actualMessage.getText());
    assertEquals(
        List.of(
            State.VIEW_POOL.getDisplayName(),
            State.CREATE_ORDER.getDisplayName(),
            State.BACK_TO_MENU.getDisplayName()),
        buttonTexts(actualMessage.getReplyMarkup()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("registerCases")
  void register_givenHandlerType_whenCalled_thenDispatchesToMatchingBotMethod(
      String caseName,
      Supplier<AbstractHandler> handlerFactory,
      boolean expectedResult,
      BiConsumer<TelegramFoodBot, AbstractHandler> verification) {
    // given
    AbstractHandler testHandler = handlerFactory.get();
    TelegramFoodBot bot = mock(TelegramFoodBot.class);
    // when
    boolean actual = testHandler.register(bot);
    // then
    assertEquals(expectedResult, actual);
    verification.accept(bot, testHandler);
  }

  static Stream<Arguments> registerCases() {
    return Stream.of(
        Arguments.of(
            "state handler",
            (Supplier<AbstractHandler>) StateHandlerDouble::new,
            true,
            (BiConsumer<TelegramFoodBot, AbstractHandler>)
                (bot, h) -> verify(bot).addStateHandler((StateHandler) h)),
        Arguments.of(
            "command handler",
            (Supplier<AbstractHandler>) CommandHandlerDouble::new,
            true,
            (BiConsumer<TelegramFoodBot, AbstractHandler>)
                (bot, h) -> verify(bot).addCommandHandler((CommandHandler) h)),
        Arguments.of(
            "callback handler",
            (Supplier<AbstractHandler>) CallbackHandlerDouble::new,
            true,
            (BiConsumer<TelegramFoodBot, AbstractHandler>)
                (bot, h) -> verify(bot).addCallbackHandler((CallbackHandler) h)),
        Arguments.of(
            "unknown handler",
            (Supplier<AbstractHandler>) UnknownHandlerDouble::new,
            false,
            (BiConsumer<TelegramFoodBot, AbstractHandler>) (bot, h) -> verifyNoInteractions(bot)));
  }

  private static class StateHandlerDouble extends AbstractHandler implements StateHandler {
    @Override
    public boolean canHandle(String state) {
      return false;
    }

    @Override
    public void handle(Update update, AbsSender sender) {}
  }

  private static class CommandHandlerDouble extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
      return false;
    }

    @Override
    public void handle(Update update, AbsSender sender) {}
  }

  private static class CallbackHandlerDouble extends AbstractHandler implements CallbackHandler {
    @Override
    public void handle(CallbackQuery callback, AbsSender sender) {}

    @Override
    public boolean canHandle(CallbackQuery callback) {
      return false;
    }
  }

  private static class UnknownHandlerDouble extends AbstractHandler {}

  private User stubUser(boolean userPresent, Status status) {
    User user = userWithStatus(status);
    when(userService.findByIdOptional(CHAT_ID_STRING))
        .thenReturn(userPresent ? Optional.of(user) : Optional.empty());
    return user;
  }

  private void stubMenu(Status status) {
    if (status == null) {
      when(menuService.existsById(CITY_KEY)).thenReturn(false);
      return;
    }
    Menu menu = menuWithStatus(status);
    when(menuService.existsById(CITY_KEY)).thenReturn(true);
    when(menuService.findById(CITY_KEY)).thenReturn(menu);
    when(menuService.findByIdOptional(CITY_KEY)).thenReturn(Optional.of(menu));
  }

  private void stubMenuWithDeadline(LocalDateTime deadline) {
    Menu menu = menuWithStatus(Status.READY);
    menu.setDeadline(deadline);
    when(menuService.existsById(CITY_KEY)).thenReturn(true);
    when(menuService.findById(CITY_KEY)).thenReturn(menu);
  }

  private static List<String> adminBaseItems(List<String> crudItems) {
    return concat(
        concat(
            USER_BASE_ITEMS,
            List.of(
                State.SEND_MESSAGE_TO_ALL_USERS.getDisplayName(),
                State.GET_TODAY_ORDERS.getDisplayName())),
        crudItems);
  }

  private static List<String> concat(List<String> first, List<String> second) {
    return Stream.concat(first.stream(), second.stream()).toList();
  }

  private static List<String> buttonTexts(ReplyKeyboard keyboard) {
    ReplyKeyboardMarkup markup = (ReplyKeyboardMarkup) keyboard;
    return markup.getKeyboard().stream()
        .flatMap(row -> row.stream().map(KeyboardButton::getText))
        .toList();
  }
}
