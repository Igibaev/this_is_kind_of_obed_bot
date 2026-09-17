/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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

  @Test
  void sendMessage_skipsReturnToMenuHint_whenTextAlreadyMentionsNavigationCommand()
      throws TelegramApiException {
    // given
    AbsSender sender = mock(AbsSender.class);
    when(sender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));
    SendMessage sendMessage = new SendMessage();
    sendMessage.setText("Чтобы отменить нажми /cancel");
    // when
    messageSender.sendMessage(sendMessage, sender);
    // then
    assertEquals("Чтобы отменить нажми /cancel", sendMessage.getText());
  }
}
