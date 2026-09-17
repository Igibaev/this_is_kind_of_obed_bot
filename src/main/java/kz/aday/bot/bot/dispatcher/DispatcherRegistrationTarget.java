/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;

public interface DispatcherRegistrationTarget {
  void addCommandHandler(CommandHandler handler);

  void addStateHandler(StateHandler handler);

  void addStateWithContentHandler(StateHandler handler);

  void addCallbackHandler(CallbackHandler handler);
}
