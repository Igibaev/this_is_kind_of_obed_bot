/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
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

class GetOrderTomorrowCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private GetOrderTomorrowCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    orderService = services.getOrderService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new GetOrderTomorrowCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsGetOrderTomorrow() {
    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDER_TOMORROW.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_sendsOrder_whenOrderExistsForTomorrow() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = new Order();
    order.getOrderItemList().add(new Item(1, "Плов", null));
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    when(orderService.findByChatIdOptional(CHAT_ID_STRING, tomorrow))
        .thenReturn(Optional.of(order));
    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDER_TOMORROW.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(
        Messages.YOUR_ORDER_IS_TOMORROW.getText(order.getOrderItemList()),
        messageCaptor.getValue().getText());
  }

  @Test
  void handle_sendsEmptyMessage_whenNoOrderForTomorrow() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    when(orderService.findByChatIdOptional(CHAT_ID_STRING, tomorrow)).thenReturn(Optional.empty());
    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDER_TOMORROW.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.ORDER_IS_EMPTY_TOMORROW.getText(), messageCaptor.getValue().getText());
  }

  @Test
  void handle_sendsEmptyMessage_whenOrderForTomorrowHasNoItems() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    Order order = new Order();
    when(orderService.findByChatIdOptional(CHAT_ID_STRING, tomorrow))
        .thenReturn(Optional.of(order));
    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDER_TOMORROW.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.ORDER_IS_EMPTY_TOMORROW.getText(), messageCaptor.getValue().getText());
  }
}
