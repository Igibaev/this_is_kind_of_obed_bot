/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.adminUser;
import static kz.aday.bot.testsupport.TestFixtures.menuTextWithDeadline;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
import static kz.aday.bot.testsupport.TestFixtures.validMenuText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.exception.TelegramMessageException;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
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

class ChangeMenuStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private ChangeMenuStateHandler handler;

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
    when(userService.findAll()).thenReturn(List.of());
    when(orderService.findAllOnDate(any())).thenReturn(List.of());

    handler = new ChangeMenuStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsChangeMenu() {
    assertTrue(handler.canHandle(State.CHANGE_MENU.getDisplayName()));
  }

  @Test
  void handle_promptsForMenuTemplate_whenStateNotYetChangeMenu() throws Exception {
    User user = adminUser(City.ALMATA);
    user.setState(State.NONE);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Update update = updateWithText(State.CHANGE_MENU.getDisplayName());

    handler.handle(update, sender);

    assertEquals(State.CHANGE_MENU, user.getState());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.MENU_TEMPLATE.getText(), messageCaptor.getValue().getText());
  }

  @Test
  void handle_savesNewMenuAndSendsPending_whenNoExistingOrdersForCity() throws Exception {
    User user = adminUser(City.ALMATA);
    user.setState(State.CHANGE_MENU);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Update update = updateWithText(validMenuText());

    handler.handle(update, sender);

    verify(menuService).save(any());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(
        Messages.MENU_PENDING.getText(user.getCity().getValue()),
        messageCaptor.getValue().getText());
  }

  @Test
  void handle_loadsOrdersForCityCurrentOrderDate_andComparesWithOldMenu_whenOrdersExist()
      throws Exception {
    User user = adminUser(City.ALMATA);
    user.setState(State.CHANGE_MENU);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(userService.findAll()).thenReturn(List.of(user));
    Order order = new Order();
    order.setChatId(user.getId());
    order.setCity(City.ALMATA);
    when(orderService.findAllOnDate(City.ALMATA.getCurrentOrderDate())).thenReturn(List.of(order));
    when(menuService.findById(City.ALMATA.toString())).thenReturn(new Menu());
    Update update = updateWithText(validMenuText());

    handler.handle(update, sender);

    verify(orderService).findAllOnDate(City.ALMATA.getCurrentOrderDate());
    verify(menuService).findById(City.ALMATA.toString());
    verify(menuService).save(any());
  }

  @Test
  void handle_throwsDuplicateCategory_andDoesNotSave_whenCategoryRepeated() {
    User user = adminUser(City.ALMATA);
    user.setState(State.CHANGE_MENU);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    Update update = updateWithText(menuTextWithDeadline("Второе:\nПлов\nВторое:\nБулочка"));

    TelegramMessageException exception =
        assertThrows(TelegramMessageException.class, () -> handler.handle(update, sender));

    assertEquals(
        Messages.MENU_CATEGORY_DUPLICATED.getText(Category.SECOND.getValue()),
        exception.getMessage());
    verify(menuService, never()).save(any());
  }
}
