/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.adminUser;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.model.City;
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

class InputMessageToAllUsersStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MessageSender messageSender;
  private AbsSender sender;
  private InputMessageToAllUsersStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new InputMessageToAllUsersStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsSendMessageToAllUsers() {
    assertTrue(handler.canHandle(State.SEND_MESSAGE_TO_ALL_USERS.getDisplayName()));
  }

  @Test
  void handle_promptsForBroadcastText_whenStateNotYetSet() throws Exception {
    User admin = adminUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));
    Update update = updateWithText(State.SEND_MESSAGE_TO_ALL_USERS.getDisplayName());

    handler.handle(update, sender);

    assertEquals(State.SEND_MESSAGE_TO_ALL_USERS, admin.getState());
    verify(messageSender).sendMessage(any(), eq(sender));
  }

  @Test
  void handle_forwardsMessageToCityUsersAndResetsState_whenAlreadyAwaitingBroadcast()
      throws Exception {
    User admin = adminUser(City.ALMATA);
    admin.setState(State.SEND_MESSAGE_TO_ALL_USERS);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));
    User recipient = readyUser(City.ALMATA);
    when(userService.findAll()).thenReturn(List.of(recipient));
    Update update = updateWithText("hello everyone");
    when(update.getMessage().hasText()).thenReturn(true);

    handler.handle(update, sender);

    assertEquals(State.NONE, admin.getState());
    verify(userService).save(admin);
  }
}
