/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class GetOrderStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private GetOrderStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    orderService = services.getOrderService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new GetOrderStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsGetOrder() {
    assertTrue(handler.canHandle(State.GET_ORDER.getDisplayName()));
  }

  @Test
  void handle_promptsForDateChoice_whenCityHasNextDayOrderCycle() throws Exception {
    User user = readyUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Update update = update();

    handler.handle(update, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.CHOOSE_DATE_ORDER.getText(), messageCaptor.getValue().getText());
  }

  @Test
  void handle_sendsCurrentOrder_whenCityHasSameDayOrderCycleAndOrderExists() throws Exception {
    User user = readyUser(City.ASTANA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ASTANA.getCurrentOrderDate()))
        .thenReturn(true);
    Order order = new Order();
    when(orderService.findByChatId(CHAT_ID_STRING, City.ASTANA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = update();

    handler.handle(update, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(
        Messages.YOUR_ORDER_IS.getText(order.getOrderItemList()),
        messageCaptor.getValue().getText());
  }
}
