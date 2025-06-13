/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface CallbackHandler {
  void handle(CallbackQuery callback, AbsSender sender) throws Exception;

  boolean canHandle(CallbackQuery callback);
}
