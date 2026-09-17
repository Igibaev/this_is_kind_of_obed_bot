/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyOrder;
import static kz.aday.bot.testsupport.TestFixtures.readyUserWithState;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
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

class ShareLunchStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private MenuService menuService;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private MessageSender messageSender;
  private AbsSender sender;
  private ShareLunchStateHandler handler;

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

    handler = new ShareLunchStateHandler();
  }

  @Test
  void handle_doesNothing_whenNoOrder() throws Exception {
    // given
    User user = readyUserWithState(State.NONE);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(false);
    Update update = updateWithText("Поделиться обедом");
    // when
    handler.handle(update, sender);
    // then
    verify(messageSender, never()).sendMessage(any(), eq(sender));
    verify(orderService, never()).deleteByChatId(any(), any());
  }

  @Test
  void handle_showsConfirmPrompt_whenStateNotYetShareLunch() throws Exception {
    // given
    User user = readyUserWithState(State.NONE);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(true);
    Order order = readyOrder();
    order.getOrderItemList().add(new Item(1, "Плов", null));
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = updateWithText("Поделиться обедом");
    // when
    handler.handle(update, sender);
    // then
    assertEquals(State.SHARE_LUNCH, user.getState());
    verify(orderService, never()).deleteByChatId(any(), any());
    verify(messageSender).sendMessage(any(), eq(sender));
  }

  @Test
  void handle_sharesOrder_whenAnsweredYesAndDeadlinePassed() throws Exception {
    // given
    User user = readyUserWithState(State.SHARE_LUNCH);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(true);
    Item item = new Item(1, "Плов", null);
    Order order = readyOrder();
    order.setSubmittedAt(LocalDateTime.now().minusMinutes(1));
    order.getOrderItemList().add(item);
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = updateWithText("Да");
    // when
    handler.handle(update, sender);
    // then
    verify(orderService).deleteByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate());
    verify(sharedOrderItemPoolService)
        .addItems(City.ALMATA, order.getDate(), CHAT_ID_STRING, "me", Set.of(item));
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals("Твой заказ расшарен: Плов.", messageCaptor.getValue().getText());
  }

  @Test
  void handle_sendsNotPossibleMessage_whenAnsweredYesButDeadlineNotPassed() throws Exception {
    // given
    User user = readyUserWithState(State.SHARE_LUNCH);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(true);
    Order order = readyOrder();
    order.getOrderItemList().add(new Item(1, "Плов", null));
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    // order.submittedAt stays null: vendor deadline not passed yet
    Update update = updateWithText("Да");
    // when
    handler.handle(update, sender);
    // then
    verify(orderService).deleteByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate());
    verify(sharedOrderItemPoolService, never()).addItems(any(), any(), any(), any(), any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals("Делиться пока нечем.", messageCaptor.getValue().getText());
  }

  @Test
  void handle_keepsOrder_whenAnsweredNo() throws Exception {
    // given
    User user = readyUserWithState(State.SHARE_LUNCH);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(orderService.existsByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(true);
    Update update = updateWithText("Нет");
    // when
    handler.handle(update, sender);
    // then
    verify(orderService, never()).deleteByChatId(any(), any());
    verify(sharedOrderItemPoolService, never()).addItems(any(), any(), any(), any(), any());
  }
}
