/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OfficeAttendanceServiceTest {

  private static final String NO_DATA_MESSAGE = "Нет данных о посещениях.";

  private Repository<OfficeAttendance> repository;
  private OfficeAttendanceService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    service = new OfficeAttendanceService();
    repository = mock(Repository.class);
    Field repositoryField = BaseService.class.getDeclaredField("repository");
    repositoryField.setAccessible(true);
    repositoryField.set(service, repository);
  }

  @Test
  void getOverallAttendanceStats_returnsNoDataMessage_whenNoAttendances() {
    // given
    when(repository.getAll()).thenReturn(List.of());
    // when
    String actual = service.getOverallAttendanceStats(City.ALMATA);
    // then
    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void getOverallAttendanceStats_returnsNoDataMessage_whenOtherCityOrNotComing() {
    // given
    when(repository.getAll())
        .thenReturn(
            List.of(
                attendance("1", "user1", City.ASTANA, true, "2026-01-05"),
                attendance("2", "user2", City.ALMATA, false, "2026-01-05")));
    // when
    String actual = service.getOverallAttendanceStats(City.ALMATA);
    // then
    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void getOverallAttendanceStats_returnsStats_whenFutureDateExcluded() {
    // given
    String tomorrow = LocalDate.now().plusDays(1).toString();
    when(repository.getAll())
        .thenReturn(
            List.of(
                attendance("1", "user1", City.ALMATA, true, "2026-01-05"),
                attendance("1", "user1", City.ALMATA, true, tomorrow)));
    // when
    String actual = service.getOverallAttendanceStats(City.ALMATA);
    // then
    assertEquals("user1: 1", actual);
  }

  @Test
  void getOverallAttendanceStats_returnsCountsSortedDescending() {
    // given
    when(repository.getAll())
        .thenReturn(
            List.of(
                attendance("1", "user1", City.ALMATA, true, "2026-01-05"),
                attendance("1", "user1", City.ALMATA, true, "2026-02-05"),
                attendance("1", "user1", City.ALMATA, true, "2026-03-05"),
                attendance("2", "user2", City.ALMATA, true, "2026-01-05"),
                attendance("3", "user3", City.ASTANA, true, "2026-01-05"),
                attendance("4", "user4", City.ALMATA, false, "2026-01-05")));
    // when
    String actual = service.getOverallAttendanceStats(City.ALMATA);
    // then
    assertEquals("user1: 3\nuser2: 1", actual);
  }

  @Test
  void getCurrentMonthAttendanceStats_returnsNoDataMessage_whenNoAttendances() {
    // given
    when(repository.getAll()).thenReturn(List.of());
    // when
    String actual = service.getCurrentMonthAttendanceStats(City.ALMATA);
    // then
    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void getCurrentMonthAttendanceStats_returnsNoDataMessage_whenOtherMonths() {
    // given
    YearMonth currentMonth = YearMonth.now();
    String pastMonthDate = currentMonth.minusMonths(1).atDay(1).toString();
    String futureMonthDate = currentMonth.plusMonths(1).atDay(1).toString();
    when(repository.getAll())
        .thenReturn(
            List.of(
                attendance("1", "user1", City.ALMATA, true, pastMonthDate),
                attendance("2", "user2", City.ALMATA, true, futureMonthDate)));
    // when
    String actual = service.getCurrentMonthAttendanceStats(City.ALMATA);
    // then
    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void getCurrentMonthAttendanceStats_returnsStats_whenFutureDateExcluded() {
    // given
    LocalDate today = LocalDate.now();
    LocalDate tomorrow = today.plusDays(1);
    assumeTrue(YearMonth.from(tomorrow).equals(YearMonth.from(today)));
    when(repository.getAll())
        .thenReturn(
            List.of(
                attendance("1", "user1", City.ALMATA, true, today.toString()),
                attendance("1", "user1", City.ALMATA, true, tomorrow.toString())));
    // when
    String actual = service.getCurrentMonthAttendanceStats(City.ALMATA);
    // then
    assertEquals("user1: 1", actual);
  }

  @Test
  void getCurrentMonthAttendanceStats_returnsCountsSortedDescending() {
    // given
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    String day1 = today.toString();
    String day2 = (yesterday.getMonth() == today.getMonth() ? yesterday : today).toString();
    when(repository.getAll())
        .thenReturn(
            List.of(
                attendance("1", "user1", City.ALMATA, true, day1),
                attendance("1", "user1", City.ALMATA, true, day2),
                attendance("2", "user2", City.ALMATA, true, day1)));
    // when
    String actual = service.getCurrentMonthAttendanceStats(City.ALMATA);
    // then
    assertEquals("user1: 2\nuser2: 1", actual);
  }

  @Test
  void getCurrentMonthAttendanceStats_returnsNoDataMessage_whenDatesMalformed() {
    // given
    when(repository.getAll())
        .thenReturn(
            List.of(
                attendance("1", "user1", City.ALMATA, true, null),
                attendance("2", "user2", City.ALMATA, true, "not-a-date")));
    // when
    String actual = service.getCurrentMonthAttendanceStats(City.ALMATA);
    // then
    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void getCurrentMonthAttendanceStats_returnsNoDataMessage_whenOtherCity() {
    // given
    YearMonth currentMonth = YearMonth.now();
    String day1 = currentMonth.atDay(1).toString();
    when(repository.getAll())
        .thenReturn(List.of(attendance("1", "user1", City.ASTANA, true, day1)));
    // when
    String actual = service.getCurrentMonthAttendanceStats(City.ALMATA);
    // then
    assertEquals(NO_DATA_MESSAGE, actual);
  }

  private static OfficeAttendance attendance(
      String chatId, String username, City city, boolean willCome, String date) {
    return new OfficeAttendance(chatId, username, city, willCome, date);
  }
}
