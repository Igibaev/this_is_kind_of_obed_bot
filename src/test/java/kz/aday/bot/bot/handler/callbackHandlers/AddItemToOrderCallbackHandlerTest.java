/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID;
import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.draftOrder;
import static kz.aday.bot.testsupport.TestFixtures.readyMenu;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

class AddItemToOrderCallbackHandlerTest {

  private static final Integer MESSAGE_ID = 42;

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OrderService orderService;
  private MenuService menuService;
  private MessageSender messageSender;
  private AbsSender sender;
  private AddItemToOrderCallbackHandler handler;

  @BeforeEach
  void setUp() {
    userService = services.getUserService();
    orderService = services.getOrderService();
    menuService = services.getMenuService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    doCallRealMethod().when(orderService).addItemToOrder(any(), any(), any());

    handler = new AddItemToOrderCallbackHandler();
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void handle_savesDraftBeforeSendingReply_forNewOrder(City city) throws Exception {
    // given
    User user = readyUser(city);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(menuService.findById(city.toString())).thenReturn(readyMenu(city));
    when(orderService.existsByChatId(CHAT_ID_STRING, city.getCurrentOrderDate())).thenReturn(false);
    stubSuccessfulSend();

    // when
    handler.handle(callbackQuery(), sender);

    // then
    InOrder order = inOrder(orderService, messageSender);
    order.verify(orderService).saveDraft(any(Order.class));
    order.verify(messageSender).sendMessage(any(), eq(sender));
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void handle_savesDraftBeforeSendingReply_forExistingOrder(City city) throws Exception {
    // given
    User user = readyUser(city);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(menuService.findById(city.toString())).thenReturn(readyMenu(city));
    when(orderService.existsByChatId(CHAT_ID_STRING, city.getCurrentOrderDate())).thenReturn(true);
    Order existing = draftOrder(city, "user");
    when(orderService.findByChatId(CHAT_ID_STRING, city.getCurrentOrderDate()))
        .thenReturn(existing);
    stubSuccessfulSend();

    // when
    handler.handle(callbackQuery(), sender);

    // then
    InOrder order = inOrder(orderService, messageSender);
    order.verify(orderService).saveDraft(eq(existing));
    order.verify(messageSender).sendMessage(any(), eq(sender));
    assertTrue(existing.getOrderItemList().stream().anyMatch(i -> i.getId().equals(1)));
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void handle_stillSavesDraft_whenSendingReplyFails(City city) throws Exception {
    // given
    User user = readyUser(city);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(menuService.findById(city.toString())).thenReturn(readyMenu(city));
    when(orderService.existsByChatId(CHAT_ID_STRING, city.getCurrentOrderDate())).thenReturn(false);
    when(messageSender.sendMessage(any(), eq(sender))).thenThrow(new TelegramApiException("boom"));

    // when / then
    assertThrows(TelegramApiException.class, () -> handler.handle(callbackQuery(), sender));

    // the item must already be on disk even though the reply never went out
    verify(orderService).saveDraft(any(Order.class));
  }

  private void stubSuccessfulSend() throws TelegramApiException {
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);
  }

  private static CallbackQuery callbackQuery() {
    CallbackQuery callback = mock(CallbackQuery.class);
    Message message = mock(Message.class);
    when(callback.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(CHAT_ID);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    when(callback.getData()).thenReturn(CallbackState.ADD_ITEM_TO_ORDER.name() + ":1");
    return callback;
  }
}
