/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.readyUserWithState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.bot.handler.stateHandlers.State;
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

class CancelCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private MessageSender messageSender;
  private AbsSender sender;
  private CancelCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    menuService = services.getMenuService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender), eq(true))).thenReturn(sentMessage);
    when(menuService.findByIdOptional(any())).thenReturn(Optional.empty());

    handler = new CancelCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsCancel() {
    CallbackQuery callback = callbackQuery(CallbackState.CANCEL.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_resetsStateAndSendsNavigationMenu() throws Exception {
    User user = readyUserWithState(State.CHANGE_MENU);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    CallbackQuery callback = callbackQuery(CallbackState.CANCEL.name());

    handler.handle(callback, sender);

    assertEquals(State.NONE, user.getState());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender), eq(true));
    assertEquals(Messages.NAVIGATION_MENU.getText(), messageCaptor.getValue().getText());
  }
}
