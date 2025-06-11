package kz.aday.bot;

import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.commandHamndlers.StartCommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.DefaultStateHandler;
import kz.aday.bot.bot.handler.stateHandlers.SetUsernameStateHandler;
import kz.aday.bot.configuration.BotConfig;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.TimeZone;

public class TelegramFoodBotApplication {
    private static final BotConfig botConfig = new BotConfig();
    public static void main(String[] args) {
        try {
            TelegramFoodBot telegramFoodBot = new TelegramFoodBot(botConfig.getBotName(), botConfig.getBotToken());
            telegramFoodBot.addCommandHandler(new StartCommandHandler());
            telegramFoodBot.addStateHandler(new SetUsernameStateHandler());
            telegramFoodBot.addStateHandler(new DefaultStateHandler());

            TimeZone.setDefault(TimeZone.getTimeZone(botConfig.getBotTimeZone()));
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramFoodBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
