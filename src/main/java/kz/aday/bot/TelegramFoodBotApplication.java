package kz.aday.bot;

import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.configuration.BotConfig;
import org.reflections.Reflections;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Set;
import java.util.TimeZone;

public class TelegramFoodBotApplication {
    private static final BotConfig botConfig = new BotConfig();
    public static void main(String[] args) {
        try {
            TelegramFoodBot telegramFoodBot = new TelegramFoodBot(botConfig.getBotName(), botConfig.getBotToken());
            addCommandsAutomatically(telegramFoodBot, AbstractHandler.class.getPackageName());

            TimeZone.setDefault(TimeZone.getTimeZone(botConfig.getBotTimeZone()));
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramFoodBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Автоматически находит и добавляет команды, реализующие BotCommand.
     * @param bot Экземпляр TelegramFoodBot.
     * @param packageName Базовый пакет для сканирования.
     */
    private static void addCommandsAutomatically(TelegramFoodBot bot, String packageName) throws Exception {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends AbstractHandler>> commandClasses = reflections.getSubTypesOf(AbstractHandler.class);

        for (Class<? extends AbstractHandler> commandClass : commandClasses) {
            // Исключаем интерфейс сам по себе
            if (commandClass.isInterface()) {
                continue;
            }
            // Создаем экземпляр команды и регистрируем ее
            AbstractHandler command = commandClass.getDeclaredConstructor().newInstance();
            if (command.register(bot)) {
                System.out.println("Registered command: " + commandClass.getSimpleName());
            }
        }
    }
}
