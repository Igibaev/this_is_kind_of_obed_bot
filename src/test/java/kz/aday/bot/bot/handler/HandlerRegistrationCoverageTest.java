/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.reflections.Reflections;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

class HandlerRegistrationCoverageTest {

  private static final Set<CallbackState> KNOWN_UNHANDLED_CALLBACK_STATES =
      Set.of(CallbackState.SUBMIT_TEMP_ORDER);
  private static final Set<State> KNOWN_UNHANDLED_STATES =
      Set.of(State.SET_CLEAR_MENU, State.SET_CANCEL);

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private List<CallbackHandler> callbackHandlers;
  private List<StateHandler> stateHandlers;
  private List<CommandHandler> commandHandlers;

  @BeforeEach
  void setUp() {
    List<AbstractHandler> handlers = instantiateAllHandlers();
    callbackHandlers =
        handlers.stream()
            .filter(h -> h instanceof CallbackHandler)
            .map(CallbackHandler.class::cast)
            .toList();
    stateHandlers =
        handlers.stream()
            .filter(h -> h instanceof StateHandler)
            .map(StateHandler.class::cast)
            .toList();
    commandHandlers =
        handlers.stream()
            .filter(h -> h instanceof CommandHandler)
            .map(CommandHandler.class::cast)
            .toList();
  }

  @ParameterizedTest
  @EnumSource(CallbackState.class)
  void everyCallbackState_hasExactlyOneHandler(CallbackState state) {
    if (KNOWN_UNHANDLED_CALLBACK_STATES.contains(state)) {
      return;
    }
    CallbackQuery callback = callbackQuery(state.name());
    long matches = callbackHandlers.stream().filter(h -> h.canHandle(callback)).count();
    assertEquals(
        1, matches, "Expected exactly one CallbackHandler for " + state + " but found " + matches);
  }

  @ParameterizedTest
  @EnumSource(
      value = State.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"NONE", "DEFAULT"})
  void everyUserFacingState_hasExactlyOneHandler(State state) {
    if (KNOWN_UNHANDLED_STATES.contains(state)) {
      return;
    }
    String stateText = state.getDisplayName();
    long matches = stateHandlers.stream().filter(h -> h.canHandle(stateText)).count();
    assertEquals(
        1, matches, "Expected exactly one StateHandler for " + state + " but found " + matches);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/start",
        "/setadmin",
        "/return",
        "/menu",
        "/getlogs",
        "/getallorders",
        "/feedback",
        "/changeorders",
        "/cancel"
      })
  void everyKnownCommand_hasExactlyOneHandler(String command) {
    long matches = commandHandlers.stream().filter(h -> h.canHandle(command)).count();
    assertEquals(
        1, matches, "Expected exactly one CommandHandler for " + command + " but found " + matches);
  }

  private static List<AbstractHandler> instantiateAllHandlers() {
    Reflections reflections = new Reflections(AbstractHandler.class.getPackageName());
    Set<Class<? extends AbstractHandler>> handlerClasses =
        reflections.getSubTypesOf(AbstractHandler.class);

    List<AbstractHandler> handlers = new ArrayList<>();
    for (Class<? extends AbstractHandler> handlerClass : handlerClasses) {
      if (handlerClass.isInterface()
          || Modifier.isAbstract(handlerClass.getModifiers())
          || handlerClass.isAnonymousClass()
          || handlerClass.isLocalClass()
          || handlerClass.isMemberClass()) {
        continue;
      }
      try {
        handlers.add(handlerClass.getDeclaredConstructor().newInstance());
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Cannot instantiate handler " + handlerClass, e);
      }
    }
    return handlers;
  }
}
