/* (C) 2024 Igibaev */
package kz.aday.bot.testsupport;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kz.aday.bot.bot.dispatcher.CallbackDispatcher;
import kz.aday.bot.bot.dispatcher.CommandDispatcher;
import kz.aday.bot.bot.dispatcher.DispatcherRegistrationTarget;
import kz.aday.bot.bot.dispatcher.DispatcherRegistry;
import kz.aday.bot.bot.dispatcher.StateDispatcher;
import kz.aday.bot.bot.dispatcher.StateWithContentDispatcher;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.configuration.ServiceContainer;
import org.reflections.Reflections;

public final class RealDispatchers implements DispatcherRegistrationTarget {

  public final CallbackDispatcher callbackDispatcher = new CallbackDispatcher();
  public final CommandDispatcher commandDispatcher = new CommandDispatcher();
  public final StateDispatcher stateDispatcher =
      new StateDispatcher(ServiceContainer.getUserService());
  public final StateWithContentDispatcher stateWithContentDispatcher =
      new StateWithContentDispatcher(ServiceContainer.getUserService());

  public RealDispatchers() {
    for (AbstractHandler handler : instantiateAllHandlers()) {
      DispatcherRegistry.register(handler, this);
    }
  }

  @Override
  public void addCommandHandler(CommandHandler handler) {
    commandDispatcher.addHandler(handler);
  }

  @Override
  public void addStateHandler(StateHandler handler) {
    stateDispatcher.addHandler(handler);
  }

  @Override
  public void addStateWithContentHandler(StateHandler handler) {
    stateWithContentDispatcher.addHandler(handler);
  }

  @Override
  public void addCallbackHandler(CallbackHandler handler) {
    callbackDispatcher.addHandler(handler);
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
