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

class GetOrderTodayAlmataCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private GetOrderTodayAlmataCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    orderService = services.getOrderService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new GetOrderTodayAlmataCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsGetOrderTodayAlmata() {
    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDER_TODAY_ALMATA.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_sendsOrder_whenOrderExistsWithItems() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = new Order();
    order.getOrderItemList().add(new Item(1, "Плов", null));
    when(orderService.findByChatIdOptional(CHAT_ID_STRING, LocalDate.now()))
        .thenReturn(Optional.of(order));
    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDER_TODAY_ALMATA.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(
        Messages.YOUR_ORDER_IS_TODAY.getText(order.getOrderItemList()),
        messageCaptor.getValue().getText());
  }

  @Test
  void handle_sendsEmptyMessage_whenNoOrderForToday() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.findByChatIdOptional(CHAT_ID_STRING, LocalDate.now()))
        .thenReturn(Optional.empty());
    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDER_TODAY_ALMATA.name());

    handler.handle(callback, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.ORDER_IS_EMPTY_TODAY.getText(), messageCaptor.getValue().getText());
  }
}
