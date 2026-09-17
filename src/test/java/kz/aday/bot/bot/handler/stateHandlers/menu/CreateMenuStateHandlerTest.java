/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.adminUser;
import static kz.aday.bot.testsupport.TestFixtures.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class CreateMenuStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private MessageSender messageSender;
  private AbsSender sender;
  private CreateMenuStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    menuService = services.getMenuService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new CreateMenuStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsCreateMenu() {
    assertTrue(handler.canHandle(State.CREATE_MENU.getDisplayName()));
  }

  @Test
  void handle_promptsMenuTemplate_whenNoMenuExistsForCity() throws Exception {
    User admin = adminUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));
    when(menuService.existsById(City.ALMATA.toString())).thenReturn(false);
    Update update = update();

    handler.handle(update, sender);

    assertEquals(State.SET_MENU, admin.getState());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.MENU_TEMPLATE.getText(), messageCaptor.getValue().getText());
  }

  @Test
  void handle_sendsReadyMenuOptions_whenMenuIsAlreadyReady() throws Exception {
    User admin = adminUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setStatus(Status.READY);
    menu.setDeadline(LocalDateTime.now().plusHours(1));
    when(menuService.existsById(City.ALMATA.toString())).thenReturn(true);
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    Update update = update();

    handler.handle(update, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(
        Messages.CREATE_MENU_READY.getText(menu.getDeadlineAsText(), City.ALMATA.getValue()),
        messageCaptor.getValue().getText());
  }
}
