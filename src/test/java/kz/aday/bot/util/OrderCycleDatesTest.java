/* (C) 2024 Igibaev */
package kz.aday.bot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderCycleDatesTest {

  private static final LocalDate FRIDAY = LocalDate.of(2026, 9, 11);
  private static final LocalDate SATURDAY = LocalDate.of(2026, 9, 12);
  private static final LocalDate SUNDAY = LocalDate.of(2026, 9, 13);
  private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);
  private static final LocalDate TUESDAY = LocalDate.of(2026, 9, 15);

  @Test
  void orderDatesForMonday_containsFridaySaturdayAndSunday() {
    assertEquals(List.of(FRIDAY, SATURDAY, SUNDAY), OrderCycleDates.orderDatesFor(MONDAY));
  }

  @Test
  void orderDatesForTuesday_containsOnlyMonday() {
    assertEquals(List.of(MONDAY), OrderCycleDates.orderDatesFor(TUESDAY));
  }

  @Test
  void orderDatesForWeekend_isEmpty() {
    assertTrue(OrderCycleDates.orderDatesFor(SATURDAY).isEmpty());
    assertTrue(OrderCycleDates.orderDatesFor(SUNDAY).isEmpty());
  }

  @Test
  void lunchDateForFridaySaturdayAndSunday_isMonday() {
    assertEquals(MONDAY, OrderCycleDates.lunchDateFor(FRIDAY));
    assertEquals(MONDAY, OrderCycleDates.lunchDateFor(SATURDAY));
    assertEquals(MONDAY, OrderCycleDates.lunchDateFor(SUNDAY));
  }

  @Test
  void lunchDateForMonday_isTuesday() {
    assertEquals(TUESDAY, OrderCycleDates.lunchDateFor(MONDAY));
  }

  @Test
  void lunchDateAndOrderDatesAreConsistent() {
    LocalDate date = LocalDate.of(2026, 1, 1);
    for (int i = 0; i < 400; i++, date = date.plusDays(1)) {
      LocalDate lunchDate = OrderCycleDates.lunchDateFor(date);
      assertTrue(OrderCycleDates.isWorkDay(lunchDate));
      assertTrue(
          OrderCycleDates.orderDatesFor(lunchDate).contains(date),
          "order date " + date + " must serve lunch on " + lunchDate);
    }
  }

  @Test
  void orderDatesForNearestLunch_onWorkDay_isTodaysLunch() {
    assertEquals(
        List.of(FRIDAY, SATURDAY, SUNDAY), OrderCycleDates.orderDatesForNearestLunch(MONDAY));
    assertEquals(List.of(MONDAY), OrderCycleDates.orderDatesForNearestLunch(TUESDAY));
  }

  @Test
  void orderDatesForNearestLunch_onWeekend_isMondaysLunch() {
    List<LocalDate> mondayLunch = List.of(FRIDAY, SATURDAY, SUNDAY);
    assertEquals(mondayLunch, OrderCycleDates.orderDatesForNearestLunch(SATURDAY));
    assertEquals(mondayLunch, OrderCycleDates.orderDatesForNearestLunch(SUNDAY));
  }

  @Test
  void nearestLunchDate_onWeekend_isMonday() {
    assertEquals(MONDAY, OrderCycleDates.nearestLunchDate(SATURDAY));
    assertEquals(MONDAY, OrderCycleDates.nearestLunchDate(SUNDAY));
  }

  @Test
  void nextLunchDate_isAfterNearestLunch() {
    assertEquals(MONDAY, OrderCycleDates.nextLunchDate(FRIDAY));
    assertEquals(TUESDAY, OrderCycleDates.nextLunchDate(SATURDAY));
    assertEquals(TUESDAY, OrderCycleDates.nextLunchDate(SUNDAY));
    assertEquals(TUESDAY, OrderCycleDates.nextLunchDate(MONDAY));
  }

  @Test
  void orderDatesForNextLunch_usesWholeWeekendForMonday() {
    assertEquals(List.of(FRIDAY, SATURDAY, SUNDAY), OrderCycleDates.orderDatesForNextLunch(FRIDAY));
    assertEquals(List.of(MONDAY), OrderCycleDates.orderDatesForNextLunch(SUNDAY));
  }

  @Test
  void formatLunchDate_usesDayMonthYear() {
    assertEquals("14.09.2026", OrderCycleDates.formatLunchDate(MONDAY));
  }

  @Test
  void orderDatesForNearestLunch_isNeverEmpty() {
    LocalDate date = LocalDate.of(2026, 1, 1);
    for (int i = 0; i < 400; i++, date = date.plusDays(1)) {
      assertTrue(
          !OrderCycleDates.orderDatesForNearestLunch(date).isEmpty(),
          "ближайший обед должен быть найден для " + date);
    }
  }

  @Test
  void isWorkDay_falseOnWeekend() {
    assertTrue(OrderCycleDates.isWorkDay(FRIDAY));
    assertTrue(OrderCycleDates.isWorkDay(MONDAY));
    assertTrue(!OrderCycleDates.isWorkDay(SATURDAY));
    assertTrue(!OrderCycleDates.isWorkDay(SUNDAY));
  }
}
