/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
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

class DeleteOrderCallbackHandlerTest {

  private static final Long CHAT_ID = 1L;
  private static final String CHAT_ID_STRING = "1";
  private static final Integer MESSAGE_ID = 42;

  private UserService userService;
  private OrderService orderService;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private MessageSender messageSender;
  private AbsSender sender;
  private DeleteOrderCallbackHandler handler;
  private MockedStatic<ServiceContainer> serviceContainer;

  @BeforeEach
  void setUp() throws Exception {
    userService = mock(UserService.class);
    orderService = mock(OrderService.class);
    sharedOrderItemPoolService = mock(SharedOrderItemPoolService.class);
    messageSender = mock(MessageSender.class);
    sender = mock(AbsSender.class);

    serviceContainer = mockStatic(ServiceContainer.class);
    serviceContainer.when(ServiceContainer::getUserService).thenReturn(userService);
    serviceContainer.when(ServiceContainer::getOrderService).thenReturn(orderService);
    serviceContainer.when(ServiceContainer::getPoolService).thenReturn(sharedOrderItemPoolService);
    serviceContainer.when(ServiceContainer::getMessageService).thenReturn(messageSender);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new DeleteOrderCallbackHandler();
  }

  @AfterEach
  void tearDown() {
    serviceContainer.close();
  }

  @Test
  void handle_doesNothing_whenNoOrder() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsById(CHAT_ID_STRING)).thenReturn(false);
    CallbackQuery callback = callbackQuery();
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService, never()).deleteById(any());
    verify(messageSender, never()).sendMessage(any(), eq(sender));
  }

  @Test
  void handle_deletesOrderAndSendsConfirmation_whenOrderExists() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsById(CHAT_ID_STRING)).thenReturn(true);
    Item item = new Item(1, "Плов", null);
    Order order = order();
    order.getOrderItemList().add(item);
    when(orderService.findById(CHAT_ID_STRING)).thenReturn(order);
    CallbackQuery callback = callbackQuery();
    // when
    handler.handle(callback, sender);
    // then
    verify(orderService).deleteById(CHAT_ID_STRING);
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

  private static Order order() {
    Order order = new Order();
    order.setChatId(CHAT_ID_STRING);
    order.setStatus(Status.READY);
    order.setDate(City.ALMATA.getCurrentOrderDate());
    return order;
  }

  private static CallbackQuery callbackQuery() {
    CallbackQuery callback = mock(CallbackQuery.class);
    Message message = mock(Message.class);
    when(callback.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(CHAT_ID);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    when(callback.getData()).thenReturn(CallbackState.DELETE_ORDER.name());
    return callback;
  }
}
