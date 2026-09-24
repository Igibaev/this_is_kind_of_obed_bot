/* (C) 2024 Igibaev */
package kz.aday.bot.scheduler;

import static kz.aday.bot.testsupport.TestFixtures.menuWithStatus;
import static kz.aday.bot.testsupport.TestFixtures.readyOrderWithItem;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

class SchedulerServiceTest {

  private static final String REPORT_HEADER_TEXT = "Список заказов.";
  private static final String EMPTY_ORDERS_TEXT = "Список заказов пуст.";
  private static final String STORAGE_FAILURE = "db is down";

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private MenuService menuService;
  private OrderService orderService;
  private UserService userService;
  private OfficeAttendanceService officeAttendanceService;
  private SharedOrderItemPoolService poolService;
  private TelegramFoodBot telegramFoodBot;
  private SchedulerService schedulerService;

  @BeforeEach
  void setUp() throws TelegramApiException {
    menuService = services.getMenuService();
    orderService = services.getOrderService();
    userService = services.getUserService();
    officeAttendanceService = services.getOfficeAttendanceService();
    poolService = services.getPoolService();

    telegramFoodBot = mock(TelegramFoodBot.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(1);
    when(telegramFoodBot.execute(any(SendMessage.class))).thenReturn(sentMessage);

    schedulerService = new SchedulerService(telegramFoodBot);
  }

  @Test
  void closeMenu_givenNextDayCycleCityWithOrderForCurrentOrderDate_thenReportsThatOrder()
      throws TelegramApiException {
    // given
    Menu menu = menuWithStatus(Status.READY);
    menu.setDeadline(LocalDateTime.now().minusMinutes(1));
    when(menuService.findAll()).thenReturn(List.of(menu));

    User user = readyUser();
    when(userService.findAll()).thenReturn(List.of(user));

    LocalDate currentOrderDate = City.ALMATA.getCurrentOrderDate();
    Order order = readyOrderWithItem(City.ALMATA, user.getPreferedName());
    order.setDate(currentOrderDate);
    when(orderService.findAllOnDate(currentOrderDate)).thenReturn(List.of(order));
    when(orderService.findAllOnDate(LocalDate.now())).thenReturn(List.of());

    // when
    schedulerService.closeMenu();

    // then
    verify(orderService).markOrdersAsSubmitted(City.ALMATA, currentOrderDate);
    List<String> sentTexts = capturedMessageTexts();
    assertTrue(sentTexts.stream().anyMatch(text -> text.contains(REPORT_HEADER_TEXT)));
    assertFalse(sentTexts.stream().anyMatch(text -> text.contains(EMPTY_ORDERS_TEXT)));
  }

  @Test
  void closeMenu_givenNextDayCycleCityWithNoOrderForCurrentOrderDate_thenSendsEmptyOrdersMessage()
      throws TelegramApiException {
    // given
    Menu menu = menuWithStatus(Status.READY);
    menu.setDeadline(LocalDateTime.now().minusMinutes(1));
    when(menuService.findAll()).thenReturn(List.of(menu));

    User user = readyUser();
    when(userService.findAll()).thenReturn(List.of(user));

    LocalDate currentOrderDate = City.ALMATA.getCurrentOrderDate();
    when(orderService.findAllOnDate(currentOrderDate)).thenReturn(List.of());

    // when
    schedulerService.closeMenu();

    // then
    List<String> sentTexts = capturedMessageTexts();
    assertTrue(sentTexts.stream().anyMatch(text -> text.contains(EMPTY_ORDERS_TEXT)));
  }

  @Test
  void closeMenu_givenDeadlineNotPassed_thenDoesNothing() throws TelegramApiException {
    // given
    Menu menu = menuWithStatus(Status.READY);
    menu.setDeadline(LocalDateTime.now().plusHours(1));
    when(menuService.findAll()).thenReturn(List.of(menu));

    // when
    schedulerService.closeMenu();

    // then
    verify(telegramFoodBot, never()).execute(any(SendMessage.class));
    verify(orderService, never()).markOrdersAsSubmitted(any(), any());
  }

  @Test
  void cleanUpStorage_consolidatesAttendanceThenDeletesOrdersPoolsAndMenusInThatOrder() {
    // when
    schedulerService.cleanUpStorage();

    // then
    InOrder inOrder = inOrder(officeAttendanceService, orderService, poolService, menuService);
    inOrder.verify(officeAttendanceService).consolidatePastMonths();
    inOrder.verify(orderService).deleteOutdated();
    inOrder.verify(poolService).deleteOutdated();
    inOrder.verify(menuService).deleteOutdated();
  }

  @Test
  void cleanUpStorage_givenAttendanceConsolidationFails_thenStillDeletesOutdatedData() {
    // given
    doThrow(new IllegalStateException(STORAGE_FAILURE))
        .when(officeAttendanceService)
        .consolidatePastMonths();

    // when
    schedulerService.cleanUpStorage();

    // then
    verify(orderService).deleteOutdated();
    verify(poolService).deleteOutdated();
    verify(menuService).deleteOutdated();
  }

  @Test
  void cleanUpStorage_givenOrdersDeletionFails_thenStillDeletesPoolsAndMenus() {
    // given
    doThrow(new IllegalStateException(STORAGE_FAILURE)).when(orderService).deleteOutdated();

    // when
    schedulerService.cleanUpStorage();

    // then
    verify(poolService).deleteOutdated();
    verify(menuService).deleteOutdated();
  }

  @Test
  void cleanUpStorage_givenPoolsDeletionFails_thenStillDeletesMenus() {
    // given
    doThrow(new IllegalStateException(STORAGE_FAILURE)).when(poolService).deleteOutdated();

    // when
    schedulerService.cleanUpStorage();

    // then
    verify(menuService).deleteOutdated();
  }

  @Test
  void cleanUpStorage_givenMenusDeletionFails_thenDoesNotPropagateException() {
    // given
    doThrow(new IllegalStateException(STORAGE_FAILURE)).when(menuService).deleteOutdated();

    // when / then
    assertDoesNotThrow(() -> schedulerService.cleanUpStorage());
  }

  private List<String> capturedMessageTexts() throws TelegramApiException {
    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(telegramFoodBot, atLeastOnce()).execute(captor.capture());
    return captor.getAllValues().stream().map(SendMessage::getText).toList();
  }
}
