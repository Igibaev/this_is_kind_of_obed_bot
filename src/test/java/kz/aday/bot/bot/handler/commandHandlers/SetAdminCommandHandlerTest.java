/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.adminUser;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.model.City;
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

class SetAdminCommandHandlerTest {

  private static final String TARGET_CHAT_ID = "5";

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MessageSender messageSender;
  private AbsSender sender;
  private SetAdminCommandHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new SetAdminCommandHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCommandStartsWithSetAdmin() {
    assertTrue(handler.canHandle("/setadmin 5"));
  }

  @Test
  void handle_promotesTargetUserToAdmin() throws Exception {
    User invokingUser = adminUser(City.ALMATA);
    User targetUser = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(invokingUser));
    when(userService.findByIdOptional(TARGET_CHAT_ID)).thenReturn(Optional.of(targetUser));
    when(userService.findById(TARGET_CHAT_ID)).thenReturn(targetUser);
    when(userService.findById(CHAT_ID_STRING)).thenReturn(invokingUser);
    Update update = updateWithText("/setadmin " + TARGET_CHAT_ID);

    handler.handle(update, sender);

    assertEquals(User.Role.ADMIN, targetUser.getRole());
    verify(userService).save(targetUser);
    verify(messageSender).sendMessage(any(), eq(sender));
  }

  @Test
  void handle_deniesAccess_whenInvokingUserIsNotAdmin() throws Exception {
    User invokingUser = readyUser();
    User targetUser = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(invokingUser));
    when(userService.findById(CHAT_ID_STRING)).thenReturn(invokingUser);
    Update update = updateWithText("/setadmin " + TARGET_CHAT_ID);

    handler.handle(update, sender);

    assertEquals(User.Role.USER, targetUser.getRole());
    verify(userService, never()).save(targetUser);
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.PERMISSION_DENIED.getText(), messageCaptor.getValue().getText());
  }
}
