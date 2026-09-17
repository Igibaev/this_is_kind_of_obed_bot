/* (C) 2024 Igibaev */
package kz.aday.bot.bot.flow;

import static kz.aday.bot.testsupport.TestFixtures.callbackQueryWithChatId;
import static kz.aday.bot.testsupport.TestFixtures.updateWithChatId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.Repository;
import kz.aday.bot.service.BaseService;
import kz.aday.bot.testsupport.AbstractPersistenceTest;
import kz.aday.bot.testsupport.RealDispatchers;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class LunchSharingFlowTest extends AbstractPersistenceTest {

  private static final long BASE_CHAT_ID = 953000000L;

  @ParameterizedTest
  @EnumSource(City.class)
  void sharerReleasesSubmittedOrder_claimerReceivesItFromPool(City city) throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long sharerChatId = chatId(city, 1);
    Long claimerChatId = chatId(city, 2);
    LocalDate orderDate = city.getCurrentOrderDate();

    seedReadyMenu(city);
    createSubmittedOrder(dispatchers, sender, sharerChatId, "Sharer", city);
    ServiceContainer.getOrderService().markOrdersAsSubmitted(city, orderDate);

    Update askToShare = updateWithChatId(sharerChatId, State.SHARE_LUNCH.getDisplayName());
    dispatchers.stateDispatcher.dispatch(askToShare, sender);
    Update confirmShare = updateWithChatId(sharerChatId, "Да");
    dispatchers.stateDispatcher.dispatch(confirmShare, sender);

    assertFalse(
        ServiceContainer.getOrderService().existsByChatId(sharerChatId.toString(), orderDate),
        "Sharer's order should be released (removed) once shared");
    List<SharedOrderItem> availableAfterShare =
        ServiceContainer.getPoolService().getAvailableEntries(city, orderDate);
    assertEquals(1, availableAfterShare.size());
    assertEquals("Плов", availableAfterShare.get(0).getItem().getName());

    Update start = updateWithChatId(claimerChatId, "/start");
    dispatchers.commandDispatcher.dispatch(start, sender);
    Update enterName = updateWithChatId(claimerChatId, "Claimer");
    dispatchers.stateDispatcher.dispatch(enterName, sender);
    Update chooseCity = updateWithChatId(claimerChatId, city.getValue());
    dispatchers.stateDispatcher.dispatch(chooseCity, sender);

    Update viewPool = updateWithChatId(claimerChatId, State.VIEW_POOL.getDisplayName());
    dispatchers.stateDispatcher.dispatch(viewPool, sender);

    String entryId =
        ServiceContainer.getPoolService().getAvailableEntries(city, orderDate).get(0).getEntryId();
    CallbackQuery claim =
        callbackQueryWithChatId(claimerChatId, CallbackState.POOL_CLAIM + ":" + entryId);
    dispatchers.callbackDispatcher.dispatch(claim, sender);

    Order claimerOrder =
        ServiceContainer.getOrderService().findByChatId(claimerChatId.toString(), orderDate);
    assertEquals(Status.READY, claimerOrder.getStatus());
    assertTrue(
        claimerOrder.getOrderItemList().stream().anyMatch(item -> "Плов".equals(item.getName())));
    assertTrue(
        ServiceContainer.getPoolService().getAvailableEntries(city, orderDate).isEmpty(),
        "Claimed entry should no longer be available");
    assertTrue(
        attendanceMarked(claimerChatId.toString(), orderDate),
        "Claiming a shared lunch should mark the claimer as attending");
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void claimingAlreadyClaimedEntry_returnsAlreadyTakenAndDoesNotDuplicateItem(City city)
      throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long sharerChatId = chatId(city, 3);
    Long firstClaimerChatId = chatId(city, 4);
    Long secondClaimerChatId = chatId(city, 5);
    LocalDate orderDate = city.getCurrentOrderDate();

    seedReadyMenu(city);
    createSubmittedOrder(dispatchers, sender, sharerChatId, "Sharer2", city);
    ServiceContainer.getOrderService().markOrdersAsSubmitted(city, orderDate);
    shareOrder(dispatchers, sender, sharerChatId);

    String entryId =
        ServiceContainer.getPoolService().getAvailableEntries(city, orderDate).get(0).getEntryId();

    createReadyUser(firstClaimerChatId, "FirstClaimer", city);
    CallbackQuery firstClaim =
        callbackQueryWithChatId(firstClaimerChatId, CallbackState.POOL_CLAIM + ":" + entryId);
    dispatchers.callbackDispatcher.dispatch(firstClaim, sender);

    createReadyUser(secondClaimerChatId, "SecondClaimer", city);
    CallbackQuery secondClaim =
        callbackQueryWithChatId(secondClaimerChatId, CallbackState.POOL_CLAIM + ":" + entryId);
    dispatchers.callbackDispatcher.dispatch(secondClaim, sender);

    assertFalse(
        ServiceContainer.getOrderService()
            .existsByChatId(secondClaimerChatId.toString(), orderDate),
        "Second claimer should not receive an order for an already-claimed entry");
    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean alreadyTakenMessageSent =
        captor.getAllValues().stream()
            .anyMatch(
                message ->
                    secondClaimerChatId.toString().equals(message.getChatId())
                        && message
                            .getText()
                            .startsWith(Messages.POOL_ITEM_ALREADY_TAKEN.getText()));
    assertTrue(alreadyTakenMessageSent);
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void decliningToShare_leavesOrderIntact(City city) throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long chatId = chatId(city, 6);
    LocalDate orderDate = city.getCurrentOrderDate();

    seedReadyMenu(city);
    createSubmittedOrder(dispatchers, sender, chatId, "Decliner", city);
    ServiceContainer.getOrderService().markOrdersAsSubmitted(city, orderDate);

    Update askToShare = updateWithChatId(chatId, State.SHARE_LUNCH.getDisplayName());
    dispatchers.stateDispatcher.dispatch(askToShare, sender);
    Update decline = updateWithChatId(chatId, "Нет");
    dispatchers.stateDispatcher.dispatch(decline, sender);

    assertTrue(ServiceContainer.getOrderService().existsByChatId(chatId.toString(), orderDate));
    assertTrue(ServiceContainer.getPoolService().getAvailableEntries(city, orderDate).isEmpty());

    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean okMessageSent =
        captor.getAllValues().stream()
            .anyMatch(
                message -> message.getText().startsWith(Messages.OK_RETURN_TO_MENU.getText()));
    assertTrue(okMessageSent);
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void sharingBeforeOrderIsMarkedSubmitted_keepsOrderIntactAndAddsNothingToPool(City city)
      throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long chatId = chatId(city, 7);
    LocalDate orderDate = city.getCurrentOrderDate();

    seedReadyMenu(city);
    createSubmittedOrder(dispatchers, sender, chatId, "TooEarly", city);
    // markOrdersAsSubmitted is intentionally NOT called here - the order's submittedAt
    // stays null, as it would be in the real gap between a user pressing "submit" and
    // the scheduler later marking the day's orders as officially submitted.

    Update askToShare = updateWithChatId(chatId, State.SHARE_LUNCH.getDisplayName());
    dispatchers.stateDispatcher.dispatch(askToShare, sender);
    Update confirmShare = updateWithChatId(chatId, "Да");
    dispatchers.stateDispatcher.dispatch(confirmShare, sender);

    assertTrue(
        ServiceContainer.getOrderService().existsByChatId(chatId.toString(), orderDate),
        "Order must not be lost when it isn't actually eligible for sharing yet");
    assertTrue(
        ServiceContainer.getPoolService().getAvailableEntries(city, orderDate).isEmpty(),
        "Nothing should have been added to the pool");

    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean notPossibleMessageSent =
        captor.getAllValues().stream()
            .anyMatch(
                message ->
                    message.getText().startsWith(Messages.SHARE_LUNCH_NOT_POSSIBLE.getText()));
    assertTrue(notPossibleMessageSent);
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void viewingPool_whenEmpty_showsEmptyMessage(City city) throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long chatId = chatId(city, 8);

    createReadyUser(chatId, "LonelyViewer", city);
    Update viewPool = updateWithChatId(chatId, State.VIEW_POOL.getDisplayName());
    dispatchers.stateDispatcher.dispatch(viewPool, sender);

    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean emptyMessageSent =
        captor.getAllValues().stream()
            .anyMatch(
                message ->
                    chatId.toString().equals(message.getChatId())
                        && message.getText().startsWith(Messages.POOL_EMPTY.getText()));
    assertTrue(emptyMessageSent);
  }

  private static Long chatId(City city, long suffix) {
    return BASE_CHAT_ID + city.ordinal() * 1000L + suffix;
  }

  private static void shareOrder(RealDispatchers dispatchers, AbsSender sender, Long chatId)
      throws Exception {
    Update askToShare = updateWithChatId(chatId, State.SHARE_LUNCH.getDisplayName());
    dispatchers.stateDispatcher.dispatch(askToShare, sender);
    Update confirmShare = updateWithChatId(chatId, "Да");
    dispatchers.stateDispatcher.dispatch(confirmShare, sender);
  }

  private static void createSubmittedOrder(
      RealDispatchers dispatchers, AbsSender sender, Long chatId, String name, City city)
      throws Exception {
    Update start = updateWithChatId(chatId, "/start");
    dispatchers.commandDispatcher.dispatch(start, sender);
    Update enterName = updateWithChatId(chatId, name);
    dispatchers.stateDispatcher.dispatch(enterName, sender);
    Update chooseCity = updateWithChatId(chatId, city.getValue());
    dispatchers.stateDispatcher.dispatch(chooseCity, sender);
    Update createOrder = updateWithChatId(chatId, State.CREATE_ORDER.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createOrder, sender);
    CallbackQuery addItem = callbackQueryWithChatId(chatId, CallbackState.ADD_ITEM_TO_ORDER + ":1");
    dispatchers.callbackDispatcher.dispatch(addItem, sender);
    CallbackQuery submitOrder = callbackQueryWithChatId(chatId, CallbackState.SUBMIT_ORDER.name());
    dispatchers.callbackDispatcher.dispatch(submitOrder, sender);
  }

  private static void createReadyUser(Long chatId, String name, City city) {
    User user =
        User.builder()
            .chatId(chatId)
            .preferedName(name)
            .city(city)
            .role(User.Role.USER)
            .status(Status.READY)
            .state(State.NONE)
            .build();
    ServiceContainer.getUserService().save(user);
  }

  @SuppressWarnings("unchecked")
  private static boolean attendanceMarked(String chatId, LocalDate date) throws Exception {
    Field repositoryField = BaseService.class.getDeclaredField("repository");
    repositoryField.setAccessible(true);
    Repository<OfficeAttendance> repository =
        (Repository<OfficeAttendance>)
            repositoryField.get(ServiceContainer.getOfficeAttendanceService());
    OfficeAttendance attendance = repository.getById(chatId + "_" + date, date);
    return attendance != null && Boolean.TRUE.equals(attendance.getWillCome());
  }

  private static void seedReadyMenu(City city) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(1, "Плов", Category.SECOND)));
    menu.setDeadline(LocalDateTime.now().plusHours(2));
    menu.setAvailable(true);
    menu.setNotificated(false);
    ServiceContainer.getMenuService().save(menu);
  }

  private static AbsSender mockSender() throws Exception {
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(sender.execute(any(SendMessage.class))).thenReturn(sentMessage);
    return sender;
  }
}
