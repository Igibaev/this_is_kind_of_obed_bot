/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.ContentAwareStateHandler;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HandlerRegistrar {

  private HandlerRegistrar() {}

  public static boolean register(Object handler, TelegramFoodBot bot) {
    if (handler instanceof StateHandler stateHandler) {
      bot.addStateHandler(stateHandler);
      if (handler instanceof ContentAwareStateHandler) {
        bot.addStateWithContentHandler(stateHandler);
      }
    } else if (handler instanceof CommandHandler commandHandler) {
      bot.addCommandHandler(commandHandler);
    } else if (handler instanceof CallbackHandler callbackHandler) {
      bot.addCallbackHandler(callbackHandler);
    } else {
      log.warn("Uknown handler: {}", handler.getClass());
      return false;
    }
    return true;
  }
}
