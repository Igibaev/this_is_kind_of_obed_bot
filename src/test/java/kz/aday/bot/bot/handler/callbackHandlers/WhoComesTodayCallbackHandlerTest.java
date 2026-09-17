/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.readyOrderWithItem;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
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

class WhoComesTodayCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private WhoComesTodayCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    orderService = services.getOrderService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new WhoComesTodayCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsWhoComesToday() {
    CallbackQuery callback = callbackQuery(CallbackState.WHO_COMES_TODAY.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_sendsNamesFromTodaysOrders_whenPresent() throws Exception {
    User user = readyUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = readyOrderWithItem(City.ALMATA, "Alice");
    when(orderService.findAllOnDate(LocalDate.now())).thenReturn(List.of(order));
    CallbackQuery callback = callbackQuery(CallbackState.WHO_COMES_TODAY.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getText().contains("Alice"));
  }

  @Test
  void handle_excludesOrdersFromOtherCity_whenPresent() throws Exception {
    User user = readyUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order sameCityOrder = readyOrderWithItem(City.ALMATA, "Alice");
    Order otherCityOrder = readyOrderWithItem(City.KARAGANDA, "Bob");
    when(orderService.findAllOnDate(LocalDate.now()))
        .thenReturn(List.of(sameCityOrder, otherCityOrder));
    CallbackQuery callback = callbackQuery(CallbackState.WHO_COMES_TODAY.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    String text = messageCaptor.getValue().getText();
    assertTrue(text.contains("Alice"));
    assertFalse(text.contains("Bob"));
  }

  @Test
  void handle_excludesNonReadyOrders_whenPresent() throws Exception {
    User user = readyUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order readyOrder = readyOrderWithItem(City.ALMATA, "Alice");
    Order pendingOrder = readyOrderWithItem(City.ALMATA, "Bob");
    pendingOrder.setStatus(Status.PENDING);
    when(orderService.findAllOnDate(LocalDate.now())).thenReturn(List.of(readyOrder, pendingOrder));
    CallbackQuery callback = callbackQuery(CallbackState.WHO_COMES_TODAY.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    String text = messageCaptor.getValue().getText();
    assertTrue(text.contains("Alice"));
    assertFalse(text.contains("Bob"));
  }

  @Test
  void handle_sendsNobodyMessage_whenNoOrdersToday() throws Exception {
    User user = readyUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.findAllOnDate(LocalDate.now())).thenReturn(List.of());
    CallbackQuery callback = callbackQuery(CallbackState.WHO_COMES_TODAY.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.NOBODY_COMES_TODAY.getText(), messageCaptor.getValue().getText());
  }
}
