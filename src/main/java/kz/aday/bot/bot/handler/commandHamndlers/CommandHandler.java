package kz.aday.bot.bot.handler.commandHamndlers;


import kz.aday.bot.bot.TelegramFoodBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface CommandHandler {
    boolean canHandle(String command);
    void handle(Update update, AbsSender sender) throws Exception;
}
