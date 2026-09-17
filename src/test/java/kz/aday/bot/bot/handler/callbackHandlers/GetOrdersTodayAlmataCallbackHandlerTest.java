/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.adminUser;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.readyOrderWithItem;
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
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

class GetOrdersTodayAlmataCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private GetOrdersTodayAlmataCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    orderService = services.getOrderService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new GetOrdersTodayAlmataCallbackHandler();
  }

  @ParameterizedTest
  @EnumSource(
      value = City.class,
      names = {"ALMATA", "KARAGANDA"})
  void handle_reportsOnlyOrdersStoredForToday_notYesterday(City city) throws Exception {
    // given
    User admin = adminUser(city);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));

    Order todayOrder = readyOrderWithItem(city, "TodayUser");
    Order yesterdayOrder = readyOrderWithItem(city, "YesterdayUser");
    when(orderService.findAllOnDate(LocalDate.now())).thenReturn(List.of(todayOrder));
    when(orderService.findAllOnDate(LocalDate.now().minusDays(1)))
        .thenReturn(List.of(yesterdayOrder));

    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDERS_TODAY_ALMATA.name());
    // when
    handler.handle(callback, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    String text = messageCaptor.getValue().getText();
    assertTrue(text.contains("TodayUser"));
    assertFalse(text.contains("YesterdayUser"));
  }

  @ParameterizedTest
  @EnumSource(
      value = City.class,
      names = {"ALMATA", "KARAGANDA"})
  void handle_reportsEmpty_whenOnlyYesterdayHasOrders(City city) throws Exception {
    // given
    User admin = adminUser(city);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));

    Order yesterdayOrder = readyOrderWithItem(city, "YesterdayUser");
    when(orderService.findAllOnDate(LocalDate.now())).thenReturn(List.of());
    when(orderService.findAllOnDate(LocalDate.now().minusDays(1)))
        .thenReturn(List.of(yesterdayOrder));

    CallbackQuery callback = callbackQuery(CallbackState.GET_ORDERS_TODAY_ALMATA.name());
    // when
    handler.handle(callback, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getText().contains(Messages.EMPTY_ORDERS_TODAY.getText()));
  }
}
