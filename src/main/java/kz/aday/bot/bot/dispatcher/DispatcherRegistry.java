/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.ContentAwareStateHandler;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;

public final class DispatcherRegistry {

  private DispatcherRegistry() {}

  public static boolean register(AbstractHandler handler, DispatcherRegistrationTarget target) {
    boolean matched = false;
    if (handler instanceof ContentAwareStateHandler contentAware) {
      target.addStateWithContentHandler(contentAware);
      matched = true;
    }
    if (handler instanceof StateHandler stateHandler) {
      target.addStateHandler(stateHandler);
      matched = true;
    } else if (handler instanceof CommandHandler commandHandler) {
      target.addCommandHandler(commandHandler);
      matched = true;
    } else if (handler instanceof CallbackHandler callbackHandler) {
      target.addCallbackHandler(callbackHandler);
      matched = true;
    }
    return matched;
  }
}
