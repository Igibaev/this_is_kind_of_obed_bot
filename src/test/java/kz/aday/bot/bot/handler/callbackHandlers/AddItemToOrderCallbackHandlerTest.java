/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Regression coverage for the bug fixed in commit ed12b52: the order draft must be persisted
 * (orderService.saveDraft) before the Telegram reply is sent, so a failed/aborted send never
 * silently drops the item the user just picked.
 */
class AddItemToOrderCallbackHandlerTest {

  private static final Long CHAT_ID = 1L;
  private static final String CHAT_ID_STRING = "1";
  private static final Integer MESSAGE_ID = 42;

  private UserService userService;
  private OrderService orderService;
  private MenuService menuService;
  private MessageSender messageSender;
  private AbsSender sender;
  private AddItemToOrderCallbackHandler handler;
  private MockedStatic<ServiceContainer> serviceContainer;

  @BeforeEach
  void setUp() {
    userService = mock(UserService.class);
    orderService = mock(OrderService.class);
    menuService = mock(MenuService.class);
    messageSender = mock(MessageSender.class);
    sender = mock(AbsSender.class);

    serviceContainer = mockStatic(ServiceContainer.class);
    serviceContainer.when(ServiceContainer::getUserService).thenReturn(userService);
    serviceContainer.when(ServiceContainer::getOrderService).thenReturn(orderService);
    serviceContainer.when(ServiceContainer::getMenuService).thenReturn(menuService);
    serviceContainer.when(ServiceContainer::getMessageService).thenReturn(messageSender);

    doCallRealMethod().when(orderService).addItemToOrder(any(), any(), any());

    handler = new AddItemToOrderCallbackHandler();
  }

  @AfterEach
  void tearDown() {
    serviceContainer.close();
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void handle_savesDraftBeforeSendingReply_forNewOrder(City city) throws Exception {
    // given
    User user = readyUser(city);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(menuService.findById(city.toString())).thenReturn(readyMenu(city));
    when(orderService.existsByChatId(CHAT_ID_STRING, city.getCurrentOrderDate()))
        .thenReturn(false);
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
    when(orderService.existsByChatId(CHAT_ID_STRING, city.getCurrentOrderDate()))
        .thenReturn(true);
    Order existing = draftOrder(city);
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
    when(orderService.existsByChatId(CHAT_ID_STRING, city.getCurrentOrderDate()))
        .thenReturn(false);
    when(messageSender.sendMessage(any(), eq(sender)))
        .thenThrow(new TelegramApiException("boom"));

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

  private static User readyUser(City city) {
    return User.builder()
        .chatId(CHAT_ID)
        .city(city)
        .role(User.Role.USER)
        .status(Status.READY)
        .preferedName("user")
        .build();
  }

  private static Menu readyMenu(City city) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(1, "Плов", Category.SECOND)));
    menu.setDeadline(LocalDateTime.now().plusHours(1));
    return menu;
  }

  private static Order draftOrder(City city) {
    Order order = new Order();
    order.setChatId(CHAT_ID_STRING);
    order.setCity(city);
    order.setUsername("user");
    order.setStatus(Status.PENDING);
    order.setDate(city.getCurrentOrderDate());
    return order;
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
