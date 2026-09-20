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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class RandomOrderStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private OrderService orderService;
  private OfficeAttendanceService officeAttendanceService;
  private MessageSender messageSender;
  private AbsSender sender;
  private RandomOrderStateHandler handler;

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

    handler = new RandomOrderStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsRandomOrder() {
    assertTrue(handler.canHandle(State.RANDOM_ORDER.getDisplayName()));
  }

  @Test
  void handle_savesDraftOrderAndAsksForConfirmation_whenStateNotYetRandomOrder() throws Exception {
    User user = readyUserWithState(State.NONE);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Update update = updateWithText(State.RANDOM_ORDER.getDisplayName());

    handler.handle(update, sender);

    assertEquals(State.RANDOM_ORDER, user.getState());
    verify(orderService).saveDraft(any());
  }

  @Test
  void handle_createsRandomOrderAndMarksAttendance_whenUserConfirms() throws Exception {
    User user = readyUserWithState(State.RANDOM_ORDER);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Order order = orderWithStatus(Status.PENDING);
    when(orderService.findByChatId(CHAT_ID_STRING, City.ALMATA.getCurrentOrderDate()))
        .thenReturn(order);
    Update update = updateWithText("Удиви меня");

    handler.handle(update, sender);

    assertEquals(State.NONE, user.getState());
    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderService).save(orderCaptor.capture());
    assertEquals(Status.READY, orderCaptor.getValue().getStatus());
    verify(officeAttendanceService).save(user.getId(), user.getCity(), true);
  }
}
