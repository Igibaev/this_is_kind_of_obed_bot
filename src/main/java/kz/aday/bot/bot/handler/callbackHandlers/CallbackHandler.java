package kz.aday.bot.bot.handler.callbackHandlers;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public abstract class CallbackHandler {
    public abstract void handle(CallbackQuery callback, AbsSender sender) throws Exception;
    public abstract boolean canHandle(CallbackQuery callback);
}
