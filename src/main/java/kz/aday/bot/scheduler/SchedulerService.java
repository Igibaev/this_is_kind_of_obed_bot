/* (C) 2024 Igibaev */
package kz.aday.bot.scheduler;

import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.OrderService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulerService {
  private final MenuService menuService = ServiceContainer.getMenuService();
  private final OrderService orderService = ServiceContainer.getOrderService();

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

  public SchedulerService() {}

  public void start() {
    planCleanTask();
    //        executorService.scheduleAtFixedRate(this::sendDeadlineIsNearNotification, 0, 5,
    // TimeUnit.SECONDS);
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
    executorService.scheduleAtFixedRate(
        this::cleanAllStorages, initialDelay, period, TimeUnit.SECONDS);
  }

  /** Закрыть меню, закрыть все заказы */
  private void closeMenu() {}

  /** Отправить уведомления за 15 минут до дедлайна */
  private void sendDeadlineIsNearNotification() {}

  /** Отчистить всю текстовую БД кроме юзеров */
  private void cleanAllStorages() {
    menuService.deleteAll();
    orderService.deleteAll();
  }
}
