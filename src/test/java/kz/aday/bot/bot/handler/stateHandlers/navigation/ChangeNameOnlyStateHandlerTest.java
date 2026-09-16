/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class ChangeNameOnlyStateHandlerTest {

  private static final Long CHAT_ID = 1L;
  private static final String CHAT_ID_STRING = "1";
  private static final Integer MESSAGE_ID = 42;

  private UserService userService;
  private MessageSender messageSender;
  private AbsSender sender;
  private ChangeNameOnlyStateHandler handler;
  private MockedStatic<ServiceContainer> serviceContainer;

  @BeforeEach
  void setUp() throws Exception {
    userService = mock(UserService.class);
    messageSender = mock(MessageSender.class);
    sender = mock(AbsSender.class);

    serviceContainer = mockStatic(ServiceContainer.class);
    serviceContainer.when(ServiceContainer::getUserService).thenReturn(userService);
    serviceContainer.when(ServiceContainer::getMessageService).thenReturn(messageSender);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new ChangeNameOnlyStateHandler();
  }

  @AfterEach
  void tearDown() {
    serviceContainer.close();
  }

  @Test
  void canHandle_givenChangeNameButtonText_whenCalled_thenReturnsTrue() {
    assertTrue(handler.canHandle(State.CHANGE_NAME_ONLY.getDisplayName()));
  }

  @Test
  void handle_promptsForNewNameWithoutTouchingCity_whenCalled() throws Exception {
    // given
    User user = readyUser();
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    Update update = updateWithChatId();
    // when
    handler.handle(update, sender);
    // then
    assertEquals(State.SET_NAME_ONLY, user.getState());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getReplyMarkup() == null);
  }

  private static User readyUser() {
    return User.builder()
        .chatId(CHAT_ID)
        .city(City.ALMATA)
        .role(User.Role.USER)
        .status(Status.READY)
        .preferedName("me")
        .build();
  }

  private static Update updateWithChatId() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    when(update.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(CHAT_ID);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    return update;
  }
}
