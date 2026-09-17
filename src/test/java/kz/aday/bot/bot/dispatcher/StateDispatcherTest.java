/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class StateDispatcherTest {

  private UserService userService;
  private StateDispatcher dispatcher;
  private AbsSender sender;

  @BeforeEach
  void setUp() {
    userService = mock(UserService.class);
    dispatcher = new StateDispatcher(userService);
    sender = mock(AbsSender.class);
  }

  @Test
  void dispatch_routesByPersistedState_whenUserHasNonNoneState() throws Exception {
    // given
    when(userService.existsById(CHAT_ID_STRING)).thenReturn(true);
    when(userService.findById(CHAT_ID_STRING))
        .thenReturn(TestFixtures.readyUserWithState(State.CHOOSE_CITY));
    StateHandler handler = mock(StateHandler.class);
    when(handler.canHandle(State.CHOOSE_CITY.getDisplayName())).thenReturn(true);
    dispatcher.addHandler(handler);
    Update update = TestFixtures.updateWithText("some raw text");
    // when
    dispatcher.dispatch(update, sender);
    // then
    verify(handler).handle(update, sender);
    verify(handler, never()).canHandle("some raw text");
  }

  @Test
  void dispatch_fallsBackToRawText_whenUserStateIsNone() throws Exception {
    // given
    when(userService.existsById(CHAT_ID_STRING)).thenReturn(true);
    when(userService.findById(CHAT_ID_STRING))
        .thenReturn(TestFixtures.readyUserWithState(State.NONE));
    StateHandler handler = mock(StateHandler.class);
    when(handler.canHandle("some raw text")).thenReturn(true);
    dispatcher.addHandler(handler);
    Update update = TestFixtures.updateWithText("some raw text");
    // when
    dispatcher.dispatch(update, sender);
    // then
    verify(handler).handle(update, sender);
  }

  @Test
  void dispatch_fallsBackToRawText_whenUserDoesNotExist() throws Exception {
    // given
    when(userService.existsById(CHAT_ID_STRING)).thenReturn(false);
    StateHandler handler = mock(StateHandler.class);
    when(handler.canHandle("some raw text")).thenReturn(true);
    dispatcher.addHandler(handler);
    Update update = TestFixtures.updateWithText("some raw text");
    // when
    dispatcher.dispatch(update, sender);
    // then
    verify(handler).handle(update, sender);
  }

  @Test
  void dispatch_throwsRuntimeException_whenNoHandlerMatches() {
    // given
    when(userService.existsById(CHAT_ID_STRING)).thenReturn(false);
    StateHandler handler = mock(StateHandler.class);
    when(handler.canHandle("unknown text")).thenReturn(false);
    dispatcher.addHandler(handler);
    Update update = TestFixtures.updateWithText("unknown text");
    // when
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> dispatcher.dispatch(update, sender));
    // then
    assertEquals(
        "Неизвестная команда [unknown text]. Вернитесь в меню /return", exception.getMessage());
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
    Update update = TestFixtures.updateWithChatId(null, "some text");
    // when / then
    assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(update, sender));
  }
}
