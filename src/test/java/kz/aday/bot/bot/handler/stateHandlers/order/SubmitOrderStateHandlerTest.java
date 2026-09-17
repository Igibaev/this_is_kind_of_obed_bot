/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.order;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.orderWithStatus;
import static kz.aday.bot.testsupport.TestFixtures.readyMenu;
import static kz.aday.bot.testsupport.TestFixtures.readyUserWithState;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
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
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
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

class SubmitOrderStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private OrderService orderService;
  private OfficeAttendanceService officeAttendanceService;
  private MessageSender messageSender;
  private AbsSender sender;
  private SubmitOrderStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    menuService = services.getMenuService();
    orderService = services.getOrderService();
    officeAttendanceService = services.getOfficeAttendanceService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    Menu menu = readyMenu(City.ALMATA);
    when(menuService.existsById(City.ALMATA.toString())).thenReturn(true);
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);

    handler = new SubmitOrderStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsSubmitOrder() {
    assertTrue(handler.canHandle(State.SUBMIT_ORDER.getDisplayName()));
  }

  @Test
  void handle_asksForConfirmation_whenStateNotYetSubmitOrder() throws Exception {
    User user = readyUserWithState(State.NONE);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = orderWithStatus(Status.PENDING);
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = updateWithText(State.SUBMIT_ORDER.getDisplayName());

    handler.handle(update, sender);

    assertEquals(State.SUBMIT_ORDER, user.getState());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(
        Messages.SUBMIT_ORDER_CONFIRM_PROMPT.getText(order.getOrderItemList()),
        messageCaptor.getValue().getText());
  }

  @Test
  void handle_confirmsOrderAndMarksAttendance_whenUserAnswersYes() throws Exception {
    User user = readyUserWithState(State.SUBMIT_ORDER);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = orderWithStatus(Status.PENDING);
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = updateWithText("Да");

    handler.handle(update, sender);

    assertEquals(State.NONE, user.getState());
    assertEquals(Status.READY, order.getStatus());
    verify(orderService).save(order);
    verify(officeAttendanceService)
        .save(user.getId(), user.getPreferedName(), user.getCity(), true);
  }
}
