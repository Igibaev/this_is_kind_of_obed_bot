package kz.aday.bot.bot.handler.commandHamndlers;


import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public abstract class CommandHandler {
    public abstract boolean canHandle(String command);
    public abstract void handle(Update update, AbsSender sender) throws Exception;
}
