/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.adminUser;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
import static kz.aday.bot.testsupport.TestFixtures.validMenuText;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class ChangeDeadlineStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private MessageSender messageSender;
  private AbsSender sender;
  private ChangeDeadlineStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    menuService = services.getMenuService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new ChangeDeadlineStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsChangeDeadline() {
    assertTrue(handler.canHandle(State.CHANGE_DEADLINE.getDisplayName()));
  }

  @Test
  void handle_updatesMenuDeadlineAndPublishesIt() throws Exception {
    User admin = adminUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setStatus(Status.PENDING);
    menu.setDeadline(LocalDateTime.now().minusHours(1));
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    Update update = updateWithText(validMenuText());

    handler.handle(update, sender);

    ArgumentCaptor<Menu> menuCaptor = ArgumentCaptor.forClass(Menu.class);
    verify(menuService).save(menuCaptor.capture());
    assertTrue(menuCaptor.getValue().getDeadline().isAfter(LocalDateTime.now()));
    verify(messageSender).sendMessage(any(), eq(sender));
  }
}
