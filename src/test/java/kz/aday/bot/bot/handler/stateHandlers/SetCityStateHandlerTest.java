/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.updateWithText;
import static kz.aday.bot.testsupport.TestFixtures.userWithStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kz.aday.bot.model.City;
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

class SetCityStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MessageSender messageSender;
  private AbsSender sender;
  private SetCityStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);
    when(messageSender.sendMessage(any(), eq(sender), eq(true))).thenReturn(sentMessage);

    handler = new SetCityStateHandler();
  }

  @Test
  void handle_showsProfileCard_whenEditingAlreadyReadyUser() throws Exception {
    // given
    User user = userWithStatus(Status.READY);
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    Update update = updateWithText(City.KARAGANDA.getValue());
    // when
    handler.handle(update, sender);
    // then
    assertEquals(City.KARAGANDA, user.getCity());
    assertEquals(State.NONE, user.getState());
    assertEquals(Status.READY, user.getStatus());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender), eq(true));
    assertTrue(messageCaptor.getValue().getReplyMarkup() != null);
    assertTrue(messageCaptor.getValue().getText().contains(City.KARAGANDA.getValue()));
  }

  @Test
  void handle_showsPlainConfirmation_whenFinishingRegistration() throws Exception {
    // given
    User user = userWithStatus(Status.PENDING);
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    Update update = updateWithText(City.KARAGANDA.getValue());
    // when
    handler.handle(update, sender);
    // then
    assertEquals(City.KARAGANDA, user.getCity());
    assertEquals(State.NONE, user.getState());
    assertEquals(Status.READY, user.getStatus());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getReplyMarkup() == null);
  }
}
