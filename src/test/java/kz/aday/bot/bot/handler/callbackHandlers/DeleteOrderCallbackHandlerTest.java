/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.readyOrder;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
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

class DeleteOrderCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private MessageSender messageSender;
  private AbsSender sender;
  private DeleteOrderCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    orderService = services.getOrderService();
    sharedOrderItemPoolService = services.getPoolService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new DeleteOrderCallbackHandler();
  }

  @Test
  void handle_doesNothing_whenNoOrder() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(false);
    CallbackQuery callback = callbackQuery(CallbackState.DELETE_ORDER.name());
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService, never()).deleteByChatId(any(), any());
    verify(messageSender, never()).sendMessage(any(), eq(sender));
  }

  @Test
  void handle_deletesOrderAndSendsConfirmation_whenOrderExists() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(true);
    Item item = new Item(1, "Плов", null);
    Order order = readyOrder();
    order.getOrderItemList().add(item);
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    CallbackQuery callback = callbackQuery(CallbackState.DELETE_ORDER.name());
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService).deleteByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate());
    verify(sharedOrderItemPoolService, never()).addItems(any(), any(), any(), any(), any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertFalse(messageCaptor.getValue().getText().contains("расшарен"));
  }
}
