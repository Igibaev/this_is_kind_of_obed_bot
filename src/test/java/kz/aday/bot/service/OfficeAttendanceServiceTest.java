/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.OfficeAttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OfficeAttendanceServiceTest {

  private static final String NO_DATA_MESSAGE = OfficeAttendanceService.NO_DATA_MESSAGE;
  private static final LocalDate OVERALL_STATS_START = OfficeAttendanceService.OVERALL_STATS_START;
  private static final String USER_ID = "1";

  private OfficeAttendanceRepository repository;
  private OfficeAttendanceService service;

  @BeforeEach
  void setUp() {
    repository = mock(OfficeAttendanceRepository.class);
    service = new OfficeAttendanceService(repository);
  }

  @Test
  void getOverallAttendanceStats_queriesCityFromOverallStartUntilToday() {
    when(repository.findAttended(City.ALMATA, OVERALL_STATS_START, LocalDate.now()))
        .thenReturn(List.of(attendance("1", "user1")));

    String actual = service.getOverallAttendanceStats(City.ALMATA);

    assertEquals("user1: 1", actual);
  }

  @Test
  void getOverallAttendanceStats_returnsNoDataMessage_whenNoAttendances() {
    when(repository.findAttended(City.ALMATA, OVERALL_STATS_START, LocalDate.now()))
        .thenReturn(List.of());

    String actual = service.getOverallAttendanceStats(City.ALMATA);

    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void getOverallAttendanceStats_returnsCountsSortedDescending() {
    when(repository.findAttended(City.ALMATA, OVERALL_STATS_START, LocalDate.now()))
        .thenReturn(
            List.of(
                attendance("2", "user2"),
                attendance("1", "user1"),
                attendance("1", "user1"),
                attendance("1", "user1")));

    String actual = service.getOverallAttendanceStats(City.ALMATA);

    assertEquals("user1: 3\nuser2: 1", actual);
  }

  @Test
  void getOverallAttendanceStats_escapesMarkdownInUsername() {
    when(repository.findAttended(City.ALMATA, OVERALL_STATS_START, LocalDate.now()))
        .thenReturn(List.of(attendance("1", "user_1")));

    String actual = service.getOverallAttendanceStats(City.ALMATA);

    assertEquals("user\\_1: 1", actual);
  }

  @Test
  void getCurrentMonthAttendanceStats_queriesCityFromFirstDayOfMonthUntilToday() {
    when(repository.findAttended(City.ALMATA, currentMonthStart(), LocalDate.now()))
        .thenReturn(List.of(attendance("1", "user1"), attendance("2", "user2")));

    String actual = service.getCurrentMonthAttendanceStats(City.ALMATA);

    assertEquals(2, actual.lines().count());
  }

  @Test
  void getCurrentMonthAttendanceStats_returnsNoDataMessage_whenNoAttendances() {
    when(repository.findAttended(City.ALMATA, currentMonthStart(), LocalDate.now()))
        .thenReturn(List.of());

    String actual = service.getCurrentMonthAttendanceStats(City.ALMATA);

    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void getOverallAttendanceStatsForUser_queriesUserFromOverallStartUntilToday() {
    when(repository.findAttendedByChatId(
            City.ALMATA, USER_ID, OVERALL_STATS_START, LocalDate.now()))
        .thenReturn(List.of(attendance(USER_ID, "user1"), attendance(USER_ID, "user1")));

    String actual = service.getOverallAttendanceStatsForUser(City.ALMATA, USER_ID);

    assertEquals("user1: 2", actual);
  }

  @Test
  void getOverallAttendanceStatsForUser_returnsNoDataMessage_whenUserHasNoAttendances() {
    when(repository.findAttendedByChatId(
            City.ALMATA, USER_ID, OVERALL_STATS_START, LocalDate.now()))
        .thenReturn(List.of());

    String actual = service.getOverallAttendanceStatsForUser(City.ALMATA, USER_ID);

    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void getCurrentMonthAttendanceStatsForUser_queriesUserFromFirstDayOfMonthUntilToday() {
    when(repository.findAttendedByChatId(
            City.ALMATA, USER_ID, currentMonthStart(), LocalDate.now()))
        .thenReturn(List.of(attendance(USER_ID, "user1")));

    String actual = service.getCurrentMonthAttendanceStatsForUser(City.ALMATA, USER_ID);

    assertEquals("user1: 1", actual);
  }

  @Test
  void getCurrentMonthAttendanceStatsForUser_returnsNoDataMessage_whenNoAttendances() {
    when(repository.findAttendedByChatId(
            City.ALMATA, USER_ID, currentMonthStart(), LocalDate.now()))
        .thenReturn(List.of());

    String actual = service.getCurrentMonthAttendanceStatsForUser(City.ALMATA, USER_ID);

    assertEquals(NO_DATA_MESSAGE, actual);
  }

  @Test
  void save_persistsAttendanceForGivenDate() {
    LocalDate date = LocalDate.of(2026, 3, 10);

    service.save(USER_ID, City.ALMATA, false, date);

    OfficeAttendance saved = capturedAttendance();
    assertEquals(USER_ID, saved.getChatId());
    assertEquals(City.ALMATA, saved.getCity());
    assertEquals(false, saved.getWillCome());
    assertEquals(date.toString(), saved.getDate());
  }

  @Test
  void save_withoutDate_persistsAttendanceForTomorrow() {
    service.save(USER_ID, City.ALMATA, true);

    assertEquals(LocalDate.now().plusDays(1).toString(), capturedAttendance().getDate());
  }

  @Test
  void findByChatId_queriesRepositoryByCompositeIdAndDate() {
    LocalDate date = LocalDate.of(2026, 3, 10);
    OfficeAttendance expected = attendance(USER_ID, "user1");
    when(repository.getById(USER_ID + "_" + date, date)).thenReturn(expected);

    assertSame(expected, service.findByChatId(USER_ID, date));
  }

  private OfficeAttendance capturedAttendance() {
    ArgumentCaptor<OfficeAttendance> captor = ArgumentCaptor.forClass(OfficeAttendance.class);
    verify(repository).save(captor.capture());
    return captor.getValue();
  }

  private static LocalDate currentMonthStart() {
    return YearMonth.now().atDay(1);
  }

  private static OfficeAttendance attendance(String chatId, String username) {
    return new OfficeAttendance(chatId, username, City.ALMATA, true, LocalDate.now().toString());
  }
}
