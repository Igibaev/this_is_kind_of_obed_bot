/* (C) 2024 Igibaev */
package kz.aday.bot.testsupport;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kz.aday.bot.bot.dispatcher.CallbackDispatcher;
import kz.aday.bot.bot.dispatcher.CommandDispatcher;
import kz.aday.bot.bot.dispatcher.StateDispatcher;
import kz.aday.bot.bot.dispatcher.StateWithContentDispatcher;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.InputMessageToAllUsersStateHandler;
import kz.aday.bot.bot.handler.stateHandlers.SendFeedbackStateHandler;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import org.reflections.Reflections;

public final class RealDispatchers {

  public final CallbackDispatcher callbackDispatcher = new CallbackDispatcher();
  public final CommandDispatcher commandDispatcher = new CommandDispatcher();
  public final StateDispatcher stateDispatcher = new StateDispatcher();
  public final StateWithContentDispatcher stateWithContentDispatcher =
      new StateWithContentDispatcher();

  public RealDispatchers() {
    for (AbstractHandler handler : instantiateAllHandlers()) {
      register(handler);
    }
  }

  private void register(AbstractHandler handler) {
    if (handler instanceof SendFeedbackStateHandler
        || handler instanceof InputMessageToAllUsersStateHandler) {
      stateWithContentDispatcher.addHandler((StateHandler) handler);
    }
    if (handler instanceof StateHandler stateHandler) {
      stateDispatcher.addHandler(stateHandler);
    } else if (handler instanceof CommandHandler commandHandler) {
      commandDispatcher.addHandler(commandHandler);
    } else if (handler instanceof CallbackHandler callbackHandler) {
      callbackDispatcher.addHandler(callbackHandler);
    }
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
