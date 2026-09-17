/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.update;
import static kz.aday.bot.testsupport.TestFixtures.userWithStatus;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class ErrorHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MessageSender messageSender;
  private AbsSender sender;
  private ErrorHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new ErrorHandler();
  }

  @Test
  void handle_sendsGoToStartMessage_whenUserNotReady() throws Exception {
    User user = userWithStatus(Status.PENDING);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    Update update = update();

    handler.handle(new RuntimeException("boom"), update, sender);

    verify(messageSender).sendMessage(any(), eq(sender));
  }

  @Test
  void handle_notifiesUserAndMainAdmin_whenUserReady() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    User mainAdmin = readyUser();
    when(userService.findById(BotConfig.getMainUserChatId())).thenReturn(mainAdmin);
    Update update = update();

    handler.handle(new RuntimeException("boom"), update, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender, times(2)).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getAllValues().get(0).getText().contains("boom"));
  }
}
