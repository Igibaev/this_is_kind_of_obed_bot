/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class SetUsernameStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MessageSender messageSender;
  private AbsSender sender;
  private SetUsernameStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new SetUsernameStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsSetUsernameThenChooseCity() {
    assertTrue(handler.canHandle(State.SET_USERNAME_THEN_CHOOSE_CITY.getDisplayName()));
  }

  @Test
  void handle_savesNameAndMovesToChooseCity() throws Exception {
    User user = readyUser();
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    Update update = updateWithText("Alice");

    handler.handle(update, sender);

    assertEquals("Alice", user.getPreferedName());
    assertEquals(State.CHOOSE_CITY, user.getState());
    verify(messageSender).sendMessage(any(), eq(sender));
  }
}
