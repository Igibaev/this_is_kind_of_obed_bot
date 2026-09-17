/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.adminUser;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

class SubmitMenuCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private MessageSender messageSender;
  private AbsSender sender;
  private SubmitMenuCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    menuService = services.getMenuService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);
    when(userService.findAll()).thenReturn(List.of());

    handler = new SubmitMenuCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsSubmitMenu() {
    CallbackQuery callback = callbackQuery(CallbackState.SUBMIT_MENU.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_publishesMenu_whenDeadlineNotPassed() throws Exception {
    User admin = adminUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setStatus(Status.PENDING);
    menu.setDeadline(LocalDateTime.now().plusHours(1));
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    CallbackQuery callback = callbackQuery(CallbackState.SUBMIT_MENU.name());

    handler.handle(callback, sender);

    ArgumentCaptor<Menu> menuCaptor = ArgumentCaptor.forClass(Menu.class);
    verify(menuService).save(menuCaptor.capture());
    assertEquals(Status.READY, menuCaptor.getValue().getStatus());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.MENU_IS_PUBLISHED.getText(), messageCaptor.getValue().getText());
  }

  @Test
  void handle_promptsNewDeadline_whenDeadlineAlreadyPassed() throws Exception {
    User admin = adminUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));
    Menu menu = new Menu();
    menu.setCity(City.ALMATA);
    menu.setStatus(Status.PENDING);
    menu.setDeadline(LocalDateTime.now().minusHours(1));
    when(menuService.findById(City.ALMATA.toString())).thenReturn(menu);
    CallbackQuery callback = callbackQuery(CallbackState.SUBMIT_MENU.name());

    handler.handle(callback, sender);

    assertEquals(State.CHANGE_DEADLINE, admin.getState());
    verify(userService, atLeastOnce()).save(admin);
    verify(menuService, never()).save(any());
  }
}
