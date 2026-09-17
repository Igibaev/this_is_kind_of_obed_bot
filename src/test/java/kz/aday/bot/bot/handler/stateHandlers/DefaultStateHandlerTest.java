/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kz.aday.bot.model.User;
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

class DefaultStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MessageSender messageSender;
  private AbsSender sender;
  private DefaultStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender), eq(true))).thenReturn(sentMessage);

    handler = new DefaultStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsDefault() {
    assertTrue(handler.canHandle(State.DEFAULT.getDisplayName()));
  }

  @Test
  void handle_sendsNavigationMenu() throws Exception {
    User user = readyUser();
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    Update update = update();

    handler.handle(update, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender), eq(true));
    assertEquals(Messages.NAVIGATION_MENU.getText(), messageCaptor.getValue().getText());
  }
}
