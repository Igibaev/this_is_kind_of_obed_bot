package kz.aday.bot.scheduler;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulerService {
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    protected SchedulerService() {
        executorService.scheduleAtFixedRate(this::cleanAllStorages, 0, 1, TimeUnit.HOURS);
        executorService.scheduleAtFixedRate(this::sendDeadlineIsNearNotification, 0, 5, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(this::closeMenu, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Закрыть меню, закрыть все заказы
     */
    private void closeMenu() {

    }

    /**
     * Отправить уведомления за 15 минут до дедлайна
     */
    private void sendDeadlineIsNearNotification() {

    }

    /**
     * Отчистить всю текстовую БД кроме юзеров
     */
    private void cleanAllStorages() {

    }
}
