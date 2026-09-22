/* (C) 2024 Igibaev */
package kz.aday.bot.bot.flow;

import static kz.aday.bot.testsupport.TestFixtures.callbackQueryWithChatId;
import static kz.aday.bot.testsupport.TestFixtures.updateWithChatId;
import static kz.aday.bot.testsupport.TestFixtures.validMenuText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
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

class MenuLifecycleFlowTest extends AbstractDbPersistenceTest {

  private static final long BASE_CHAT_ID = 952000000L;

  @ParameterizedTest
  @EnumSource(City.class)
  void adminPublishesMenu_userOrders_thenAdminClearsMenu_leavesConsistentState(City city)
      throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long adminChatId = chatId(city, 1);
    Long userChatId = chatId(city, 2);

    resetMenu(city);
    seedReadyAdmin(adminChatId, city);

    Update createMenu = updateWithChatId(adminChatId, State.CREATE_MENU.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createMenu, sender);

    Update setMenuText = updateWithChatId(adminChatId, validMenuText());
    dispatchers.stateDispatcher.dispatch(setMenuText, sender);

    CallbackQuery publishMenu =
        callbackQueryWithChatId(adminChatId, CallbackState.SUBMIT_MENU.name());
    dispatchers.callbackDispatcher.dispatch(publishMenu, sender);

    assertTrue(ServiceContainer.getMenuService().existsById(city.toString()));
    assertTrue(
        ServiceContainer.getMenuService().findById(city.toString()).getStatus() == Status.READY);

    Update start = updateWithChatId(userChatId, "/start");
    dispatchers.commandDispatcher.dispatch(start, sender);

    Update enterName = updateWithChatId(userChatId, "Мария");
    dispatchers.stateDispatcher.dispatch(enterName, sender);

    Update chooseCity = updateWithChatId(userChatId, city.getValue());
    dispatchers.stateDispatcher.dispatch(chooseCity, sender);

    Update createOrder = updateWithChatId(userChatId, State.CREATE_ORDER.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createOrder, sender);

    CallbackQuery addItem =
        callbackQueryWithChatId(userChatId, CallbackState.ADD_ITEM_TO_ORDER + ":1");
    dispatchers.callbackDispatcher.dispatch(addItem, sender);

    CallbackQuery submitOrder =
        callbackQueryWithChatId(userChatId, CallbackState.SUBMIT_ORDER.name());
    dispatchers.callbackDispatcher.dispatch(submitOrder, sender);

    LocalDate orderDate = city.getCurrentOrderDate();
    assertTrue(ServiceContainer.getOrderService().existsByChatId(userChatId.toString(), orderDate));

    CallbackQuery clearMenu = callbackQueryWithChatId(adminChatId, CallbackState.CLEAR_MENU.name());
    dispatchers.callbackDispatcher.dispatch(clearMenu, sender);

    assertFalse(ServiceContainer.getMenuService().existsById(city.toString()));
    assertFalse(
        ServiceContainer.getOrderService().existsByChatId(userChatId.toString(), orderDate));
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void nonAdminUser_cannotCreateMenu_getsPermissionDeniedAndNoMenuCreated(City city)
      throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long nonAdminChatId = chatId(city, 10);
    resetMenu(city);
    seedReadyUser(nonAdminChatId, city, User.Role.USER);

    Update createMenu = updateWithChatId(nonAdminChatId, State.CREATE_MENU.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createMenu, sender);

    assertFalse(ServiceContainer.getMenuService().existsById(city.toString()));
    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean permissionDeniedSent =
        captor.getAllValues().stream()
            .anyMatch(
                message ->
                    nonAdminChatId.toString().equals(message.getChatId())
                        && message.getText().startsWith(Messages.PERMISSION_DENIED.getText()));
    assertTrue(permissionDeniedSent, "Expected non-admin to receive a permission-denied message");
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void publishingMenu_afterDeadlinePassed_staysPendingAndPromptsForNewDeadline(City city)
      throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long adminChatId = chatId(city, 1);
    resetMenu(city);
    seedReadyAdmin(adminChatId, city);

    Update createMenu = updateWithChatId(adminChatId, State.CREATE_MENU.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createMenu, sender);
    Update setMenuText = updateWithChatId(adminChatId, validMenuText());
    dispatchers.stateDispatcher.dispatch(setMenuText, sender);

    Menu menuWithExpiredDeadline = ServiceContainer.getMenuService().findById(city.toString());
    menuWithExpiredDeadline.setDeadline(LocalDateTime.now().minusMinutes(1));
    ServiceContainer.getMenuService().save(menuWithExpiredDeadline);

    CallbackQuery publishMenu =
        callbackQueryWithChatId(adminChatId, CallbackState.SUBMIT_MENU.name());
    dispatchers.callbackDispatcher.dispatch(publishMenu, sender);

    assertEquals(
        Status.PENDING,
        ServiceContainer.getMenuService().findById(city.toString()).getStatus(),
        "Menu should not be published once its deadline has already passed");
    assertEquals(
        State.CHANGE_DEADLINE,
        ServiceContainer.getUserService().findById(adminChatId.toString()).getState(),
        "Admin should be prompted to set a new deadline");

    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean expiredMessageSent =
        captor.getAllValues().stream()
            .anyMatch(message -> message.getText().contains("дедлайн уже прошел"));
    assertTrue(expiredMessageSent, "Expected an already-expired deadline message");
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void invalidMenuText_isRejected_menuStaysUnsetAndAdminStaysInSetMenuState(City city)
      throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long adminChatId = chatId(city, 1);
    resetMenu(city);
    seedReadyAdmin(adminChatId, city);

    Update createMenu = updateWithChatId(adminChatId, State.CREATE_MENU.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createMenu, sender);

    Update invalidMenuText = updateWithChatId(adminChatId, "Просто текст без меню и дедлайна");
    dispatchers.stateDispatcher.dispatch(invalidMenuText, sender);

    assertFalse(ServiceContainer.getMenuService().existsById(city.toString()));
    assertEquals(
        State.SET_MENU,
        ServiceContainer.getUserService().findById(adminChatId.toString()).getState(),
        "Admin should remain in SET_MENU waiting for a corrected message");
    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    boolean parseErrorSent =
        captor.getAllValues().stream()
            .anyMatch(message -> message.getText().startsWith("Дедлайн некорректный"));
    assertTrue(parseErrorSent, "Expected a deadline-parsing error message");
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void clearingMenu_deletesOrdersOnlyForThatCity_leavesOtherCitiesUntouched(City city)
      throws Exception {
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    Long adminChatId = chatId(city, 1);
    City otherCity = anotherCity(city);
    String otherCityChatId = chatId(otherCity, 11).toString();
    resetMenu(city);
    seedReadyAdmin(adminChatId, city);

    Update createMenu = updateWithChatId(adminChatId, State.CREATE_MENU.getDisplayName());
    dispatchers.stateDispatcher.dispatch(createMenu, sender);
    Update setMenuText = updateWithChatId(adminChatId, validMenuText());
    dispatchers.stateDispatcher.dispatch(setMenuText, sender);
    CallbackQuery publishMenu =
        callbackQueryWithChatId(adminChatId, CallbackState.SUBMIT_MENU.name());
    dispatchers.callbackDispatcher.dispatch(publishMenu, sender);

    Order otherCityOrder = new Order();
    otherCityOrder.setChatId(otherCityChatId);
    otherCityOrder.setUsername("OtherCityUser");
    otherCityOrder.setCity(otherCity);
    otherCityOrder.setStatus(Status.READY);
    otherCityOrder.setDate(LocalDate.now());
    ServiceContainer.getOrderService().save(otherCityOrder);

    CallbackQuery clearMenu = callbackQueryWithChatId(adminChatId, CallbackState.CLEAR_MENU.name());
    dispatchers.callbackDispatcher.dispatch(clearMenu, sender);

    assertFalse(ServiceContainer.getMenuService().existsById(city.toString()));
    assertTrue(
        ServiceContainer.getOrderService().existsByChatId(otherCityChatId, LocalDate.now()),
        "Order from a different city must survive clearing this city's menu");
  }

  private static City anotherCity(City city) {
    City[] cities = City.values();
    return cities[(city.ordinal() + 1) % cities.length];
  }

  private static Long chatId(City city, long suffix) {
    return BASE_CHAT_ID + city.ordinal() * 1000L + suffix;
  }

  private static void resetMenu(City city) {
    ServiceContainer.getMenuService().deleteById(city.toString());
  }

  private static void seedReadyAdmin(Long chatId, City city) {
    seedReadyUser(chatId, city, User.Role.ADMIN);
  }

  private static void seedReadyUser(Long chatId, City city, User.Role role) {
    User user =
        User.builder()
            .chatId(chatId)
            .preferedName(role == User.Role.ADMIN ? "Admin" : "User")
            .city(city)
            .role(role)
            .status(Status.READY)
            .state(State.NONE)
            .build();
    ServiceContainer.getUserService().save(user);
  }

  private static AbsSender mockSender() throws Exception {
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(sender.execute(any(SendMessage.class))).thenReturn(sentMessage);
    return sender;
  }
}
