/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

class SharedOrderItemPoolClaimCallbackHandlerTest {

  private static final Long CHAT_ID = 1L;
  private static final String CHAT_ID_STRING = "1";
  private static final Integer MESSAGE_ID = 42;
  private static final LocalDate TARGET_DATE = LocalDate.now();

  private UserService userService;
  private OrderService orderService;
  private OfficeAttendanceService officeAttendanceService;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private MessageSender messageSender;
  private AbsSender sender;
  private PoolClaimCallbackHandler handler;
  private MockedStatic<ServiceContainer> serviceContainer;

  @BeforeEach
  void setUp() throws Exception {
    userService = mock(UserService.class);
    orderService = mock(OrderService.class);
    officeAttendanceService = mock(OfficeAttendanceService.class);
    sharedOrderItemPoolService = mock(SharedOrderItemPoolService.class);
    messageSender = mock(MessageSender.class);
    MenuService menuService = mock(MenuService.class);
    sender = mock(AbsSender.class);

    serviceContainer = mockStatic(ServiceContainer.class);
    serviceContainer.when(ServiceContainer::getUserService).thenReturn(userService);
    serviceContainer.when(ServiceContainer::getOrderService).thenReturn(orderService);
    serviceContainer
        .when(ServiceContainer::getOfficeAttendanceService)
        .thenReturn(officeAttendanceService);
    serviceContainer.when(ServiceContainer::getPoolService).thenReturn(sharedOrderItemPoolService);
    serviceContainer.when(ServiceContainer::getMessageService).thenReturn(messageSender);
    serviceContainer.when(ServiceContainer::getMenuService).thenReturn(menuService);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new PoolClaimCallbackHandler();
  }

  @AfterEach
  void tearDown() {
    serviceContainer.close();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsPoolClaim() {
    // given
    CallbackQuery callback = callbackQuery("POOL_CLAIM:e1");
    // when
    boolean actual = handler.canHandle(callback);
    // then
    assertTrue(actual);
  }

  @Test
  void handle_addsItemToNewOrderAndMarksAttendanceToday_whenNoExistingOrder() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Item item = new Item(1, "Плов", Category.FIRST);
    SharedOrderItem entry = new SharedOrderItem("e1", item, "9", "otherUser", null, null);
    when(sharedOrderItemPoolService.claim(City.ALMATA, TARGET_DATE, "e1", CHAT_ID_STRING, "me"))
        .thenReturn(Optional.of(entry));
    when(orderService.findByChatIdOptional(CHAT_ID_STRING, TARGET_DATE))
        .thenReturn(Optional.empty());
    when(sharedOrderItemPoolService.getAvailableEntries(City.ALMATA, TARGET_DATE))
        .thenReturn(List.of());
    CallbackQuery callback = callbackQuery("POOL_CLAIM:e1");
    // when
    handler.handle(callback, sender);
    // then
    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderService).save(orderCaptor.capture());
    Order savedOrder = orderCaptor.getValue();
    assertEquals(CHAT_ID_STRING, savedOrder.getChatId());
    assertEquals(Status.READY, savedOrder.getStatus());
    assertEquals(TARGET_DATE, savedOrder.getDate());
    assertTrue(savedOrder.getOrderItemList().contains(item));
    verify(officeAttendanceService).save(CHAT_ID_STRING, "me", City.ALMATA, true, LocalDate.now());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getText().contains("Плов"));
  }

  @Test
  void handle_addsItemToExistingOrder_whenOrderAlreadyPresent() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Item newItem = new Item(2, "Лагман", Category.FIRST);
    Item existingItem = new Item(1, "Плов", Category.FIRST);
    SharedOrderItem entry = new SharedOrderItem("e1", newItem, "9", "otherUser", null, null);
    when(sharedOrderItemPoolService.claim(City.ALMATA, TARGET_DATE, "e1", CHAT_ID_STRING, "me"))
        .thenReturn(Optional.of(entry));
    Order existingOrder = new Order();
    existingOrder.setChatId(CHAT_ID_STRING);
    existingOrder.setStatus(Status.READY);
    existingOrder.setDate(TARGET_DATE);
    existingOrder.getOrderItemList().add(existingItem);
    when(orderService.findByChatIdOptional(CHAT_ID_STRING, TARGET_DATE))
        .thenReturn(Optional.of(existingOrder));
    when(sharedOrderItemPoolService.getAvailableEntries(City.ALMATA, TARGET_DATE))
        .thenReturn(List.of());
    CallbackQuery callback = callbackQuery("POOL_CLAIM:e1");
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService).save(existingOrder);
    assertTrue(existingOrder.getOrderItemList().containsAll(List.of(existingItem, newItem)));
  }

  @Test
  void handle_showsAlreadyTakenAndRemainingEntries_whenClaimFails() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(sharedOrderItemPoolService.claim(City.ALMATA, TARGET_DATE, "e1", CHAT_ID_STRING, "me"))
        .thenReturn(Optional.empty());
    SharedOrderItem remaining =
        new SharedOrderItem(
            "e2", new Item(3, "Хлеб", Category.FIRST), "9", "otherUser", null, null);
    when(sharedOrderItemPoolService.getAvailableEntries(City.ALMATA, TARGET_DATE))
        .thenReturn(List.of(remaining));
    CallbackQuery callback = callbackQuery("POOL_CLAIM:e1");
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService, never()).save(any());
    verify(officeAttendanceService, never()).save(any(), any(), any(), anyBoolean(), any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getText().contains("уже забрали"));
  }

  private static User readyUser() {
    return User.builder()
        .chatId(CHAT_ID)
        .city(City.ALMATA)
        .role(User.Role.USER)
        .status(Status.READY)
        .preferedName("me")
        .build();
  }

  private static CallbackQuery callbackQuery(String data) {
    CallbackQuery callback = mock(CallbackQuery.class);
    Message message = mock(Message.class);
    when(callback.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(CHAT_ID);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    when(callback.getData()).thenReturn(data);
    return callback;
  }
}
