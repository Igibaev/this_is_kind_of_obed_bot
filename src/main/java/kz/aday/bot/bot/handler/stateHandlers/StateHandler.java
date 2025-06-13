/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface StateHandler {
  boolean canHandle(String state);

  void handle(Update update, AbsSender sender) throws Exception;
}
