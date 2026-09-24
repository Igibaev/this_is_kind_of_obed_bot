/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

class MessageSenderTest {

  private static final String TEXT = "Ваше имя: me \nГород: Алматы";

  private final MessageSender messageSender = new MessageSender();

  @Test
  void sendMessage_appendsReturnToMenuHint_whenCalledWithoutSuppressFlag() throws Exception {
    // given
    AbsSender sender = mock(AbsSender.class);
    when(sender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));
    SendMessage sendMessage = new SendMessage();
    sendMessage.setText(TEXT);
    // when
    messageSender.sendMessage(sendMessage, sender);
    // then
    assertEquals(TEXT + "\nЧтобы вернуться в меню нажмите /menu", sendMessage.getText());
  }

  @Test
  void sendMessage_appendsReturnToMenuHint_whenSuppressFlagIsFalse() throws Exception {
    // given
    AbsSender sender = mock(AbsSender.class);
    when(sender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));
    SendMessage sendMessage = new SendMessage();
    sendMessage.setText(TEXT);
    // when
    messageSender.sendMessage(sendMessage, sender, false);
    // then
    assertEquals(TEXT + "\nЧтобы вернуться в меню нажмите /menu", sendMessage.getText());
  }

  @Test
  void sendMessage_skipsReturnToMenuHint_whenSuppressFlagIsTrue() throws Exception {
    // given
    AbsSender sender = mock(AbsSender.class);
    when(sender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));
    SendMessage sendMessage = new SendMessage();
    sendMessage.setText(TEXT);
    // when
    messageSender.sendMessage(sendMessage, sender, true);
    // then
    assertEquals(TEXT, sendMessage.getText());
  }

  @ParameterizedTest
  @FieldSource("kz.aday.bot.service.MessageSender#NAVIGATION_COMMANDS")
  void sendMessage_skipsReturnToMenuHint_whenTextAlreadyMentionsNavigationCommand(String command)
      throws TelegramApiException {
    AbsSender sender = mock(AbsSender.class);
    when(sender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));
    SendMessage sendMessage = new SendMessage();
    String text = "Нажми " + command;
    sendMessage.setText(text);

    messageSender.sendMessage(sendMessage, sender);

    assertEquals(text, sendMessage.getText());
  }

  @Test
  void sendMessage_sendsWithoutHint_whenTextIsNull() throws TelegramApiException {
    AbsSender sender = mock(AbsSender.class);
    when(sender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));
    SendMessage sendMessage = new SendMessage();

    messageSender.sendMessage(sendMessage, sender);

    assertNull(sendMessage.getText());
    verify(sender).execute(sendMessage);
  }

  @Test
  void sendMessage_rethrows_whenTelegramFails() throws TelegramApiException {
    AbsSender sender = mock(AbsSender.class);
    TelegramApiException failure = new TelegramApiException("boom");
    when(sender.execute(any(SendMessage.class))).thenThrow(failure);
    SendMessage sendMessage = new SendMessage();
    sendMessage.setText(TEXT);

    assertSame(
        failure,
        assertThrows(
            TelegramApiException.class, () -> messageSender.sendMessage(sendMessage, sender)));
  }

  @Test
  void deleteMessage_deletesEachNonNullMessageId_andIgnoresFailures() throws TelegramApiException {
    AbsSender sender = mock(AbsSender.class);
    when(sender.executeAsync(any(DeleteMessage.class))).thenThrow(new TelegramApiException("gone"));

    messageSender.deleteMessage(1L, Arrays.asList(10, null, 11), sender);

    ArgumentCaptor<DeleteMessage> captor = ArgumentCaptor.forClass(DeleteMessage.class);
    verify(sender, times(2)).executeAsync(captor.capture());
    assertEquals(
        List.of(10, 11), captor.getAllValues().stream().map(DeleteMessage::getMessageId).toList());
  }

  @Test
  void deleteMessage_doesNothing_whenListEmpty() throws TelegramApiException {
    AbsSender sender = mock(AbsSender.class);

    messageSender.deleteMessage(1L, List.of(), sender);

    verify(sender, never()).executeAsync(any(DeleteMessage.class));
  }
}
