/* (C) 2024 Igibaev */
package kz.aday.bot.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Календарь для городов со сдвинутым циклом заказа (Алматы, Караганда): меню публикуется сегодня, а
 * обед по нему привозят на следующий рабочий день.
 *
 * <p>Выходные (суббота и воскресенье) рабочими днями не считаются, поэтому меню, созданное в
 * пятницу, субботу или воскресенье, обслуживает обед в понедельник.
 */
public final class OrderCycleDates {

  private OrderCycleDates() {}

  /** Рабочий ли день (всё, кроме субботы и воскресенья). */
  public static boolean isWorkDay(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
  }

  /**
   * День обеда для заказов, сделанных в {@code orderDate}, — ближайший рабочий день после него.
   * Пятница, суббота и воскресенье дают понедельник.
   */
  public static LocalDate lunchDateFor(LocalDate orderDate) {
    LocalDate lunchDate = orderDate.plusDays(1);
    while (!isWorkDay(lunchDate)) {
      lunchDate = lunchDate.plusDays(1);
    }
    return lunchDate;
  }

  /**
   * Дни, заказы за которые обслуживают обед в {@code lunchDate}, в хронологическом порядке. Для
   * понедельника это пятница, суббота и воскресенье, для остальных рабочих дней — предыдущий день.
   * Для выходного дня обедов нет, список пустой.
   */
  public static List<LocalDate> orderDatesFor(LocalDate lunchDate) {
    if (!isWorkDay(lunchDate)) {
      return List.of();
    }
    List<LocalDate> orderDates = new ArrayList<>();
    LocalDate date = lunchDate.minusDays(1);
    while (!isWorkDay(date)) {
      orderDates.add(date);
      date = date.minusDays(1);
    }
    orderDates.add(date);
    Collections.sort(orderDates);
    return orderDates;
  }

  /** Дни, заказы за которые обслуживают сегодняшний обед. */
  public static List<LocalDate> orderDatesForToday() {
    return orderDatesFor(LocalDate.now());
  }
}
