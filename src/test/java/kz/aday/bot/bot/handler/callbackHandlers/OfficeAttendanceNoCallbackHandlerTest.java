/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

class OfficeAttendanceNoCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OfficeAttendanceService officeAttendanceService;
  private OrderService orderService;
  private MenuService menuService;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private MessageSender messageSender;
  private AbsSender sender;
  private OfficeAttendanceNoCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    officeAttendanceService = services.getOfficeAttendanceService();
    orderService = services.getOrderService();
    menuService = services.getMenuService();
    sharedOrderItemPoolService = services.getPoolService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new OfficeAttendanceNoCallbackHandler();
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
    verify(officeAttendanceService).save(CHAT_ID_STRING, City.ALMATA, false, LocalDate.now());
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
        .save(CHAT_ID_STRING, City.ALMATA, false, LocalDate.now().plusDays(1));
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
    order.setSubmittedAt(LocalDateTime.now().minusMinutes(1));
    order.getOrderItemList().add(item);
    when(orderService.existsByChatId(CHAT_ID_STRING, tomorrow)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, tomorrow)).thenReturn(order);
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
    // order.submittedAt stays null: vendor deadline not passed yet
    CallbackQuery callback = callbackQuery("ATTENDANCE_NO:TOMORROW");
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService, never()).deleteByChatId(CHAT_ID_STRING, tomorrow);
    verify(sharedOrderItemPoolService, never()).addItems(any(), any(), any(), any(), any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertFalse(messageCaptor.getValue().getText().contains("расшарен"));
  }

  @Test
  void handle_sharesOrderToPool_whenTodayAndOrderSubmittedToVendor() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    LocalDate today = LocalDate.now();
    Item item = new Item(1, "Плов", null);
    Order order = new Order();
    order.setChatId(CHAT_ID_STRING);
    order.setStatus(Status.READY);
    order.setDate(today);
    order.setSubmittedAt(LocalDateTime.now().minusHours(12));
    order.getOrderItemList().add(item);
    when(orderService.existsByChatId(CHAT_ID_STRING, today)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, today)).thenReturn(order);
    CallbackQuery callback = callbackQuery("ATTENDANCE_NO:TODAY");
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService).deleteByChatId(CHAT_ID_STRING, today);
    verify(sharedOrderItemPoolService)
        .addItems(City.ALMATA, today, CHAT_ID_STRING, "me", Set.of(item));
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getText().contains("расшарен"));
  }

  @Test
  void handle_doesNotShareOrder_whenTodayButOrderNotYetSubmittedToVendor() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    LocalDate today = LocalDate.now();
    Order order = new Order();
    order.setChatId(CHAT_ID_STRING);
    order.setStatus(Status.READY);
    order.setDate(today);
    order.getOrderItemList().add(new Item(1, "Плов", null));
    when(orderService.existsByChatId(CHAT_ID_STRING, today)).thenReturn(true);
    when(orderService.findByChatId(CHAT_ID_STRING, today)).thenReturn(order);
    // order.submittedAt stays null: not yet submitted to vendor
    CallbackQuery callback = callbackQuery("ATTENDANCE_NO:TODAY");
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService, never()).deleteByChatId(CHAT_ID_STRING, today);
    verify(sharedOrderItemPoolService, never()).addItems(any(), any(), any(), any(), any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertFalse(messageCaptor.getValue().getText().contains("расшарен"));
  }
}
