/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class NoneCallbackHandler extends AbstractHandler implements CallbackHandler {
  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {}

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.NONE);
  }
}
