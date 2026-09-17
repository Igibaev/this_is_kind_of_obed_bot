/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyUserWithState;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.model.User;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class SendFeedbackStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private AbsSender sender;
  private SendFeedbackStateHandler handler;

  @BeforeEach
  void setUp() {
    userService = services.getUserService();
    sender = mock(AbsSender.class);

    handler = new SendFeedbackStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsSendFeedback() {
    assertTrue(handler.canHandle(State.SEND_FEEDBACK.getDisplayName()));
  }

  @Test
  void handle_forwardsMessageToMainUser_whenUserHasContent() throws Exception {
    User user = readyUserWithState(State.SEND_FEEDBACK);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    Update update = updateWithText("bug report");
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage().hasText()).thenReturn(true);

    handler.handle(update, sender);

    verify(sender).executeAsync(any(ForwardMessage.class));
  }

  @Test
  void handle_doesNothing_whenMessageHasNoContent() throws Exception {
    User user = readyUserWithState(State.SEND_FEEDBACK);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    Update update = updateWithText("");
    when(update.hasMessage()).thenReturn(true);

    handler.handle(update, sender);

    verify(sender, never()).executeAsync(any(ForwardMessage.class));
  }
}
