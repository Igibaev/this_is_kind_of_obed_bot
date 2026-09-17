/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyOrder;
import static kz.aday.bot.testsupport.TestFixtures.readyUserWithState;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class DeleteOrderStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private MenuService menuService;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private MessageSender messageSender;
  private AbsSender sender;
  private DeleteOrderStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    orderService = services.getOrderService();
    menuService = services.getMenuService();
    sharedOrderItemPoolService = services.getPoolService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setStatus(Status.READY);
    menu.setDeadline(LocalDateTime.now().plusHours(1));
    when(menuService.existsById(City.ALMATA.toString())).thenReturn(true);
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);

    handler = new DeleteOrderStateHandler();
  }

  @Test
  void handle_showsConfirmPrompt_whenStateNotYetDeleteOrder() throws Exception {
    // given
    User user = readyUserWithState(State.NONE);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = readyOrder();
    order.getOrderItemList().add(new Item(1, "Плов", null));
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = updateWithText("Удалить заказ");
    // when
    handler.handle(update, sender);
    // then
    assertEquals(State.DELETE_ORDER, user.getState());
    verify(orderService, never()).deleteByChatId(any(), any());
    verify(messageSender).sendMessage(any(), eq(sender));
  }

  @Test
  void handle_deletesOrder_whenAnsweredYes() throws Exception {
    // given
    User user = readyUserWithState(State.DELETE_ORDER);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = readyOrder();
    order.getOrderItemList().add(new Item(1, "Плов", null));
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = updateWithText("Да");
    // when
    handler.handle(update, sender);
    // then
    verify(orderService).deleteByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate());
    verify(sharedOrderItemPoolService, never()).addItems(any(), any(), any(), any(), any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertFalse(messageCaptor.getValue().getText().contains("расшарен"));
  }

  @Test
  void handle_keepsOrder_whenAnsweredNo() throws Exception {
    // given
    User user = readyUserWithState(State.DELETE_ORDER);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = readyOrder();
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = updateWithText("Нет");
    // when
    handler.handle(update, sender);
    // then
    verify(orderService, never()).deleteByChatId(any(), any());
    verify(sharedOrderItemPoolService, never()).addItems(any(), any(), any(), any(), any());
  }
}
