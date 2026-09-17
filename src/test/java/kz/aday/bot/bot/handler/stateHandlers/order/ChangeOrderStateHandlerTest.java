/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.orderWithStatus;
import static kz.aday.bot.testsupport.TestFixtures.readyMenu;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
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

class ChangeOrderStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private ChangeOrderStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    menuService = services.getMenuService();
    orderService = services.getOrderService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new ChangeOrderStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsChangeOrder() {
    assertTrue(handler.canHandle(State.CHANGE_ORDER.getDisplayName()));
  }

  @Test
  void handle_sendsCurrentOrderWithEditButtons_whenMenuReadyAndOrderExists() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Menu menu = readyMenu(City.ALMATA);
    when(menuService.existsById(City.ALMATA.toString())).thenReturn(true);
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(true);
    Order order = orderWithStatus(Status.READY);
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = update();

    handler.handle(update, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(
        Messages.YOUR_ORDER_IS.getText(order.getOrderItemList()),
        messageCaptor.getValue().getText());
  }

  @Test
  void handle_doesNothing_whenNoOrderExists() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(menuService.existsById(City.ALMATA.toString())).thenReturn(true);
    Menu menu = readyMenu(City.ALMATA);
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(false);
    Update update = update();

    handler.handle(update, sender);

    verify(messageSender, never()).sendMessage(any(), eq(sender));
  }
}
