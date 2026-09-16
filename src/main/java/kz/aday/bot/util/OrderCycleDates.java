/* (C) 2024 Igibaev */
package kz.aday.bot.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

  private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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

  /**
   * Дни, заказы за которые обслуживают ближайший обед: сегодняшний, если сегодня рабочий день,
   * иначе обед ближайшего рабочего дня. В выходные это даёт заказы на понедельник — в субботу и
   * воскресенье их всё равно нужно видеть.
   */
  public static List<LocalDate> orderDatesForToday() {
    return orderDatesForNearestLunch(LocalDate.now());
  }

  /** То же самое для произвольного дня. */
  public static List<LocalDate> orderDatesForNearestLunch(LocalDate today) {
    return orderDatesFor(nearestLunchDate(today));
  }

  /** Сегодняшний обед в рабочий день или ближайший обед после выходных. */
  public static LocalDate nearestLunchDate(LocalDate today) {
    return isWorkDay(today) ? today : lunchDateFor(today);
  }

  /** Рабочий обед, следующий за ближайшим доступным обедом. */
  public static LocalDate nextLunchDate(LocalDate today) {
    return lunchDateFor(nearestLunchDate(today));
  }

  /** Дни заказов для рабочего обеда, следующего за ближайшим доступным обедом. */
  public static List<LocalDate> orderDatesForNextLunch(LocalDate today) {
    return orderDatesFor(nextLunchDate(today));
  }

  /** Дата обеда для пользовательских сообщений. */
  public static String formatLunchDate(LocalDate lunchDate) {
    return lunchDate.format(DISPLAY_DATE);
  }
}
