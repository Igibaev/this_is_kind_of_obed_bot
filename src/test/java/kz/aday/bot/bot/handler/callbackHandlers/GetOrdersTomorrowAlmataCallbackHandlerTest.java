/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

class GetOrdersTomorrowAlmataCallbackHandlerTest {

  private static final Long CHAT_ID = 1L;
  private static final String CHAT_ID_STRING = "1";
  private static final Integer MESSAGE_ID = 42;

  private UserService userService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private GetOrdersTomorrowAlmataCallbackHandler handler;
  private MockedStatic<ServiceContainer> serviceContainer;

  @BeforeEach
  void setUp() throws Exception {
    userService = mock(UserService.class);
    orderService = mock(OrderService.class);
    messageSender = mock(MessageSender.class);
    sender = mock(AbsSender.class);

    serviceContainer = mockStatic(ServiceContainer.class);
    serviceContainer.when(ServiceContainer::getUserService).thenReturn(userService);
    serviceContainer.when(ServiceContainer::getOrderService).thenReturn(orderService);
    serviceContainer.when(ServiceContainer::getMessageService).thenReturn(messageSender);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new GetOrdersTomorrowAlmataCallbackHandler();
  }

  @AfterEach
  void tearDown() {
    serviceContainer.close();
  }

  @ParameterizedTest
  @EnumSource(
      value = City.class,
      names = {"ALMATA", "KARAGANDA"})
  void handle_reportsOnlyOrdersStoredForTomorrow_notToday(City city) throws Exception {
    // given
    User admin = adminUser(city);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));

    Order todayOrder = readyOrderWithItem(city, "TodayUser");
    Order tomorrowOrder = readyOrderWithItem(city, "TomorrowUser");
    when(orderService.findAllOnDate(LocalDate.now())).thenReturn(List.of(todayOrder));
    when(orderService.findAllOnDate(LocalDate.now().plusDays(1)))
        .thenReturn(List.of(tomorrowOrder));

    CallbackQuery callback = callbackQuery();
    // when
    handler.handle(callback, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    String text = messageCaptor.getValue().getText();
    assertTrue(text.contains("TomorrowUser"));
    assertFalse(text.contains("TodayUser"));
  }

  @ParameterizedTest
  @EnumSource(
      value = City.class,
      names = {"ALMATA", "KARAGANDA"})
  void handle_reportsEmpty_whenOnlyTodayHasOrders(City city) throws Exception {
    // given
    User admin = adminUser(city);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));

    Order todayOrder = readyOrderWithItem(city, "TodayUser");
    when(orderService.findAllOnDate(LocalDate.now())).thenReturn(List.of(todayOrder));
    when(orderService.findAllOnDate(LocalDate.now().plusDays(1))).thenReturn(List.of());

    CallbackQuery callback = callbackQuery();
    // when
    handler.handle(callback, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(
        messageCaptor.getValue().getText().contains(Messages.EMPTY_ORDERS_TOMORROW.getText()));
  }

  private static User adminUser(City city) {
    return User.builder()
        .chatId(CHAT_ID)
        .city(city)
        .role(User.Role.ADMIN)
        .status(Status.READY)
        .preferedName("admin")
        .build();
  }

  private static Order readyOrderWithItem(City city, String username) {
    Order order = new Order();
    order.setChatId(CHAT_ID_STRING);
    order.setCity(city);
    order.setUsername(username);
    order.setStatus(Status.READY);
    order.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    return order;
  }

  private static CallbackQuery callbackQuery() {
    CallbackQuery callback = mock(CallbackQuery.class);
    Message message = mock(Message.class);
    when(callback.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(CHAT_ID);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    when(callback.getData()).thenReturn(CallbackState.GET_ORDERS_TOMORROW_ALMATA.name());
    return callback;
  }
}
