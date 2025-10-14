/* (C) 2024 Igibaev */
package kz.aday.bot;

import java.util.Set;
import java.util.TimeZone;
import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.scheduler.SchedulerService;
import org.reflections.Reflections;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramFoodBotApplication {

  public static void main(String[] args) {
    try {
      System.out.println("FDfsfsdfd");
      SchedulerService schedulerService = new SchedulerService();
      schedulerService.start();
      TelegramFoodBot telegramFoodBot =
          new TelegramFoodBot(BotConfig.getBotName(), BotConfig.getBotToken());
      addCommandsAutomatically(telegramFoodBot, AbstractHandler.class.getPackageName());

      TimeZone.setDefault(TimeZone.getTimeZone(BotConfig.getBotTimeZone()));
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
   *
   * @param bot Экземпляр TelegramFoodBot.
   * @param packageName Базовый пакет для сканирования.
   */
  private static void addCommandsAutomatically(TelegramFoodBot bot, String packageName)
      throws Exception {
    Reflections reflections = new Reflections(packageName);
    Set<Class<? extends AbstractHandler>> commandClasses =
        reflections.getSubTypesOf(AbstractHandler.class);

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
