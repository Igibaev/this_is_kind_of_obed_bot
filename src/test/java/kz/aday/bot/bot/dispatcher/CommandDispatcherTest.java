/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class CommandDispatcherTest {

  private CommandDispatcher dispatcher;
  private AbsSender sender;

  @BeforeEach
  void setUp() {
    dispatcher = new CommandDispatcher();
    sender = mock(AbsSender.class);
  }

  @Test
  void dispatch_callsMatchingHandler_whenCommandRecognized() throws Exception {
    // given
    CommandHandler handler = mock(CommandHandler.class);
    when(handler.canHandle("/start")).thenReturn(true);
    dispatcher.addHandler(handler);
    Update update = TestFixtures.updateWithText("/start");
    // when
    dispatcher.dispatch(update, sender);
    // then
    verify(handler).handle(update, sender);
  }

  @Test
  void dispatch_stopsAtFirstMatchingHandler_whenMultipleHandlersRegistered() throws Exception {
    // given
    CommandHandler matching = mock(CommandHandler.class);
    CommandHandler nonMatching = mock(CommandHandler.class);
    when(matching.canHandle("/start")).thenReturn(true);
    when(nonMatching.canHandle("/start")).thenReturn(false);
    dispatcher.addHandler(matching);
    dispatcher.addHandler(nonMatching);
    Update update = TestFixtures.updateWithText("/start");
    // when
    dispatcher.dispatch(update, sender);
    // then
    verify(matching).handle(update, sender);
    verify(nonMatching, never()).handle(update, sender);
  }

  @Test
  void dispatch_trimsCommandText_beforeMatching() throws Exception {
    // given
    CommandHandler handler = mock(CommandHandler.class);
    when(handler.canHandle("/start")).thenReturn(true);
    dispatcher.addHandler(handler);
    Update update = TestFixtures.updateWithText("  /start  ");
    // when
    dispatcher.dispatch(update, sender);
    // then
    verify(handler).canHandle("/start");
  }

  @Test
  void dispatch_throwsRuntimeException_withOriginalCommandText_whenNoHandlerMatches() {
    // given
    CommandHandler handler = mock(CommandHandler.class);
    when(handler.canHandle("/unknown")).thenReturn(false);
    dispatcher.addHandler(handler);
    Update update = TestFixtures.updateWithText("/unknown");
    // when
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> dispatcher.dispatch(update, sender));
    // then
    assertEquals(
        "Неизвестная команда [/unknown]. Вернитесь в меню /return", exception.getMessage());
  }

  @Test
  void dispatch_throwsIllegalArgumentException_whenUpdateIsNull() {
    assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(null, sender));
  }

  @Test
  void dispatch_throwsIllegalArgumentException_whenMessageTextIsNull() {
    // given
    Update update = TestFixtures.update();
    // when / then
    assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(update, sender));
  }

  @Test
  void dispatch_throwsIllegalArgumentException_whenChatIdIsNull() {
    // given
    Update update = TestFixtures.updateWithChatId(null, "/start");
    // when / then
    assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(update, sender));
  }
}
