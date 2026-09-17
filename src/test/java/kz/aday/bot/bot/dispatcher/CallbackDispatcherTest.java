/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

class CallbackDispatcherTest {

  private CallbackDispatcher dispatcher;
  private AbsSender sender;

  @BeforeEach
  void setUp() {
    dispatcher = new CallbackDispatcher();
    sender = mock(AbsSender.class);
  }

  @Test
  void dispatch_callsMatchingHandler_whenCallbackRecognized() throws Exception {
    // given
    CallbackQuery callback = TestFixtures.callbackQuery("ORDER:1");
    CallbackHandler handler = mock(CallbackHandler.class);
    when(handler.canHandle(callback)).thenReturn(true);
    dispatcher.addHandler(handler);
    // when
    dispatcher.dispatch(callback, sender);
    // then
    verify(handler).handle(callback, sender);
  }

  @Test
  void dispatch_stopsAtFirstMatchingHandler_whenMultipleHandlersRegistered() throws Exception {
    // given
    CallbackQuery callback = TestFixtures.callbackQuery("ORDER:1");
    CallbackHandler matching = mock(CallbackHandler.class);
    CallbackHandler nonMatching = mock(CallbackHandler.class);
    when(matching.canHandle(callback)).thenReturn(true);
    when(nonMatching.canHandle(callback)).thenReturn(false);
    dispatcher.addHandler(matching);
    dispatcher.addHandler(nonMatching);
    // when
    dispatcher.dispatch(callback, sender);
    // then
    verify(matching).handle(callback, sender);
    verify(nonMatching, never()).handle(callback, sender);
  }

  @Test
  void dispatch_throwsRuntimeException_usingMessageTextNotCallbackData_whenNoHandlerMatches() {
    // given
    CallbackQuery callback = TestFixtures.callbackQuery("ORDER:1");
    when(callback.getMessage().getText()).thenReturn("previous bot message");
    CallbackHandler handler = mock(CallbackHandler.class);
    when(handler.canHandle(callback)).thenReturn(false);
    dispatcher.addHandler(handler);
    // when
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> dispatcher.dispatch(callback, sender));
    // then
    assertEquals(
        "Неизвестная команда [previous bot message]. Вернитесь в меню /return",
        exception.getMessage());
  }

  @Test
  void dispatch_rethrowsSameExceptionInstance_whenHandlerThrows() throws Exception {
    // given
    CallbackQuery callback = TestFixtures.callbackQuery("ORDER:1");
    CallbackHandler handler = mock(CallbackHandler.class);
    when(handler.canHandle(callback)).thenReturn(true);
    RuntimeException original = new RuntimeException("boom");
    doThrow(original).when(handler).handle(callback, sender);
    dispatcher.addHandler(handler);
    // when
    Exception thrown = assertThrows(Exception.class, () -> dispatcher.dispatch(callback, sender));
    // then
    assertSame(original, thrown);
  }

  @Test
  void dispatch_throwsIllegalArgumentException_whenCallbackDataIsNull() {
    // given
    CallbackQuery callback = mock(CallbackQuery.class);
    Message message = mock(Message.class);
    when(callback.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(TestFixtures.CHAT_ID);
    // when / then
    assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(callback, sender));
  }

  @Test
  void dispatch_throwsIllegalArgumentException_whenChatIdIsNull() {
    // given
    CallbackQuery callback = TestFixtures.callbackQueryWithChatId(null, "ORDER:1");
    // when / then
    assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(callback, sender));
  }
}
