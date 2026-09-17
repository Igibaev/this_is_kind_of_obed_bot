/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.orderWithStatus;
import static kz.aday.bot.testsupport.TestFixtures.readyMenu;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

class SubmitOrderCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private OrderService orderService;
  private OfficeAttendanceService officeAttendanceService;
  private MessageSender messageSender;
  private AbsSender sender;
  private SubmitOrderCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    menuService = services.getMenuService();
    orderService = services.getOrderService();
    officeAttendanceService = services.getOfficeAttendanceService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new SubmitOrderCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsSubmitOrder() {
    CallbackQuery callback = callbackQuery(CallbackState.SUBMIT_ORDER.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_marksOrderReadyAndMarksAttendance_whenDeadlineNotPassed() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Menu menu = readyMenu(City.ALMATA);
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    Order order = orderWithStatus(Status.PENDING);
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    CallbackQuery callback = callbackQuery(CallbackState.SUBMIT_ORDER.name());

    handler.handle(callback, sender);

    assertEquals(Status.READY, order.getStatus());
    verify(orderService).save(order);
    verify(officeAttendanceService)
        .save(user.getId(), user.getPreferedName(), user.getCity(), true);
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(
        Messages.ORDER_SENDED.getText(order.getOrderItemList()),
        messageCaptor.getValue().getText());
  }

  @Test
  void handle_doesNotSubmitOrder_whenDeadlineAlreadyPassed() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setStatus(Status.DEADLINE);
    menu.setDeadline(LocalDateTime.now().minusHours(1));
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    Order order = orderWithStatus(Status.PENDING);
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    CallbackQuery callback = callbackQuery(CallbackState.SUBMIT_ORDER.name());

    handler.handle(callback, sender);

    assertEquals(Status.PENDING, order.getStatus());
    verify(orderService, never()).save(any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.MENU_DEADLINE_IS_PASSED.getText(), messageCaptor.getValue().getText());
  }
}
