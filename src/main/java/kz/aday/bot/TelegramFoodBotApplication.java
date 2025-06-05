package kz.aday.bot;

import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.configuration.BotConfig;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.LocalDateTime;
import java.util.TimeZone;

public class TelegramFoodBotApplication {
    private static final BotConfig botConfig = new BotConfig();
    public static void main(String[] args) {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(botConfig.getBotTimeZone()));
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(
                    new TelegramFoodBot(botConfig.getBotName(), botConfig.getBotToken())
            );
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
