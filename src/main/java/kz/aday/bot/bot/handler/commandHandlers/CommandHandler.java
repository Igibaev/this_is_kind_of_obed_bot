/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHandlers;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface CommandHandler {
  boolean canHandle(String command);

  void handle(Update update, AbsSender sender) throws Exception;
}
