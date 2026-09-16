/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
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

class OfficeAttendanceNoCallbackHandlerTest {

  private static final Long CHAT_ID = 1L;
  private static final String CHAT_ID_STRING = "1";
  private static final Integer MESSAGE_ID = 42;

  private UserService userService;
  private OfficeAttendanceService officeAttendanceService;
  private OrderService orderService;
  private MenuService menuService;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private MessageSender messageSender;
  private AbsSender sender;
  private OfficeAttendanceNoCallbackHandler handler;
  private MockedStatic<ServiceContainer> serviceContainer;

  @BeforeEach
  void setUp() throws Exception {
    userService = mock(UserService.class);
    officeAttendanceService = mock(OfficeAttendanceService.class);
    orderService = mock(OrderService.class);
    menuService = mock(MenuService.class);
    sharedOrderItemPoolService = mock(SharedOrderItemPoolService.class);
    messageSender = mock(MessageSender.class);
    sender = mock(AbsSender.class);

    serviceContainer = mockStatic(ServiceContainer.class);
    serviceContainer.when(ServiceContainer::getUserService).thenReturn(userService);
    serviceContainer
        .when(ServiceContainer::getOfficeAttendanceService)
        .thenReturn(officeAttendanceService);
    serviceContainer.when(ServiceContainer::getOrderService).thenReturn(orderService);
    serviceContainer.when(ServiceContainer::getMenuService).thenReturn(menuService);
    serviceContainer.when(ServiceContainer::getPoolService).thenReturn(sharedOrderItemPoolService);
    serviceContainer.when(ServiceContainer::getMessageService).thenReturn(messageSender);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new OfficeAttendanceNoCallbackHandler();
  }

  @AfterEach
  void tearDown() {
    serviceContainer.close();
  }

  @Test
  void handle_savesAttendanceForToday_whenTodayFlagSet() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    CallbackQuery callback = callbackQuery("ATTENDANCE_NO:TODAY");
    // when
    handler.handle(callback, sender);
    // then
    verify(officeAttendanceService).save(CHAT_ID_STRING, "me", City.ALMATA, false, LocalDate.now());
  }

  @Test
  void handle_savesAttendanceForTomorrow_whenTodayFlagAbsent() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    CallbackQuery callback = callbackQuery("ATTENDANCE_NO:TOMORROW");
    // when
    handler.handle(callback, sender);
    // then
    verify(officeAttendanceService)
        .save(CHAT_ID_STRING, "me", City.ALMATA, false, LocalDate.now().plusDays(1));
  }

  @Test
  void handle_sharesOrderToPool_whenTomorrowAndOrderExistsAndDeadlinePassed() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    Item item = new Item(1, "Плов", null);
    Order order = new Order();
    order.setChatId(CHAT_ID_STRING);
    order.setStatus(Status.READY);
    order.setDate(tomorrow);
    order.getOrderItemList().add(item);
    when(orderService.existsByChatId(CHAT_ID_STRING, tomorrow)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, tomorrow)).thenReturn(order);
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setStatus(Status.DEADLINE);
    menu.setDeadline(LocalDateTime.now().minusMinutes(1));
    when(menuService.existsById(City.ALMATA.toString())).thenReturn(true);
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    CallbackQuery callback = callbackQuery("ATTENDANCE_NO:TOMORROW");
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService).deleteByChatId(CHAT_ID_STRING, tomorrow);
    verify(sharedOrderItemPoolService)
        .addItems(City.ALMATA, tomorrow, CHAT_ID_STRING, "me", Set.of(item));
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getText().contains("расшарен"));
  }

  @Test
  void handle_doesNotShareOrder_whenTomorrowButDeadlineNotPassed() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    Order order = new Order();
    order.setChatId(CHAT_ID_STRING);
    order.setStatus(Status.READY);
    order.setDate(tomorrow);
    order.getOrderItemList().add(new Item(1, "Плов", null));
    when(orderService.existsByChatId(CHAT_ID_STRING, tomorrow)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, tomorrow)).thenReturn(order);
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setStatus(Status.READY);
    menu.setDeadline(LocalDateTime.now().plusMinutes(1));
    when(menuService.existsById(City.ALMATA.toString())).thenReturn(true);
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    CallbackQuery callback = callbackQuery("ATTENDANCE_NO:TOMORROW");
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService).deleteByChatId(CHAT_ID_STRING, tomorrow);
    verify(sharedOrderItemPoolService, never()).addItems(any(), any(), any(), any(), any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertFalse(messageCaptor.getValue().getText().contains("расшарен"));
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
