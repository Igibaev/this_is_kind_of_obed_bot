package kz.aday.bot.scheduler;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulerService {
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public SchedulerService() {
    }

    public void start() {
        planCleanTask();
//        executorService.scheduleAtFixedRate(this::sendDeadlineIsNearNotification, 0, 5, TimeUnit.SECONDS);
//        executorService.scheduleAtFixedRate(this::closeMenu, 0, 5, TimeUnit.SECONDS);
    }

    private void planCleanTask() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.with(LocalTime.MIDNIGHT);
        if (now.isAfter(next)) {
            next = next.plusDays(1);
        }

        Duration duration = Duration.between(now, next);
        long initialDelay = duration.getSeconds();

        long period = TimeUnit.DAYS.toSeconds(1);
        executorService.scheduleAtFixedRate(this::cleanAllStorages, initialDelay, period, TimeUnit.SECONDS);
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
        LocalDateTime now = LocalDateTime.of(LocalDate.now(), LocalTime.of(23,33,0));
        System.out.println(now.toLocalTime());
        System.out.println(now.toLocalTime().getHour());
        System.out.println(now.toLocalTime().getHour() == 23);
    }
}
