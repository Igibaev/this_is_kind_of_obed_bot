/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.Test;

class OfficeAttendanceRepositoryTest extends AbstractDbPersistenceTest {

  private static final LocalDate TODAY = LocalDate.now();
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);
  private static final LocalDate PERIOD_START = LocalDate.of(2001, 2, 1);
  private static final LocalDate PERIOD_END = LocalDate.of(2001, 2, 28);

  private final OfficeAttendanceRepository repository =
      new OfficeAttendanceRepository(PersistenceConfig.getDataSource());

  @Test
  void existById_returnsFalse_whenAttendanceWasNeverSaved() {
    assertFalse(repository.existById("940000001_" + TODAY, TODAY));
  }

  @Test
  void getById_returnsNull_whenAttendanceWasNeverSaved() {
    assertNull(repository.getById("940000002_" + TODAY, TODAY));
  }

  @Test
  void getAll_returnsEveryPreviouslySavedAttendance() {
    OfficeAttendance first = buildAttendance("940000003", City.ALMATA, TODAY);
    OfficeAttendance second = buildAttendance("940000004", City.ASTANA, TODAY);

    repository.save(first);
    repository.save(second);

    Collection<OfficeAttendance> all = repository.getAll();

    assertTrue(all.contains(first));
    assertTrue(all.contains(second));
  }

  @Test
  void getAllByDate_returnsOnlyAttendancesMatchingThatDate() {
    OfficeAttendance today = buildAttendance("940000005", City.ALMATA, TODAY);
    OfficeAttendance yesterday = buildAttendance("940000005", City.ALMATA, YESTERDAY);

    repository.save(today);
    repository.save(yesterday);

    Collection<OfficeAttendance> todayAttendances = repository.getAll(TODAY);

    assertTrue(todayAttendances.contains(today));
    assertFalse(todayAttendances.contains(yesterday));
  }

  @Test
  void save_updatesExistingRow_insteadOfDuplicatingIt_whenIdAlreadyExists() {
    OfficeAttendance original = buildAttendance("940000006", City.ALMATA, TODAY);
    repository.save(original);

    OfficeAttendance updated = buildAttendance("940000006", City.ASTANA, TODAY);
    updated.setWillCome(false);
    repository.save(updated);

    OfficeAttendance found = repository.getById(original.getId(), TODAY);
    assertEquals(City.ASTANA, found.getCity());
    assertEquals(false, found.getWillCome());
    assertEquals(
        1, repository.getAll().stream().filter(a -> a.getId().equals(original.getId())).count());
  }

  @Test
  void deleteById_removesAttendance() {
    OfficeAttendance attendance = buildAttendance("940000007", City.ALMATA, TODAY);
    repository.save(attendance);

    repository.deleteById(attendance.getId(), TODAY);

    assertNull(repository.getById(attendance.getId(), TODAY));
  }

  @Test
  void findAttended_returnsOnlyComingAttendancesOfCityWithinPeriod() {
    String chatId = "940000008";
    String notComingChatId = "940000009";
    OfficeAttendance inPeriod = saveAttendance(chatId, City.ALMATA, PERIOD_START.plusDays(1));
    saveAttendance(chatId, City.ASTANA, PERIOD_START.plusDays(2));
    saveAttendance(chatId, City.ALMATA, PERIOD_START.minusDays(1));
    saveAttendance(chatId, City.ALMATA, PERIOD_END.plusDays(1));
    OfficeAttendance notComing =
        buildAttendance(notComingChatId, City.ALMATA, PERIOD_START.plusDays(1));
    notComing.setWillCome(false);
    repository.save(notComing);

    List<OfficeAttendance> found =
        ofChatIds(
            repository.findAttended(City.ALMATA, PERIOD_START, PERIOD_END),
            chatId,
            notComingChatId);

    assertEquals(List.of(inPeriod), found);
  }

  @Test
  void findAttended_includesAttendancesOnPeriodBoundaries() {
    String chatId = "940000010";
    OfficeAttendance onStart = saveAttendance(chatId, City.KARAGANDA, PERIOD_START);
    OfficeAttendance onEnd = saveAttendance(chatId, City.KARAGANDA, PERIOD_END);

    List<OfficeAttendance> found =
        ofChatIds(repository.findAttended(City.KARAGANDA, PERIOD_START, PERIOD_END), chatId);

    assertEquals(Set.of(onStart, onEnd), Set.copyOf(found));
  }

  @Test
  void findAttendedByChatId_returnsOnlyAttendancesOfRequestedUser() {
    String chatId = "940000011";
    String otherChatId = "940000012";
    OfficeAttendance own = saveAttendance(chatId, City.ALMATA, PERIOD_START.plusDays(3));
    saveAttendance(otherChatId, City.ALMATA, PERIOD_START.plusDays(3));

    Collection<OfficeAttendance> found =
        repository.findAttendedByChatId(City.ALMATA, chatId, PERIOD_START, PERIOD_END);

    assertEquals(List.of(own), List.copyOf(found));
  }

  @Test
  void findAttendedByChatId_excludesNotComingOtherCityAndOutOfPeriodAttendances() {
    String chatId = "940000013";
    saveAttendance(chatId, City.ASTANA, PERIOD_START.plusDays(4));
    saveAttendance(chatId, City.ALMATA, PERIOD_END.plusDays(2));
    OfficeAttendance notComing = buildAttendance(chatId, City.ALMATA, PERIOD_START.plusDays(5));
    notComing.setWillCome(false);
    repository.save(notComing);

    Collection<OfficeAttendance> found =
        repository.findAttendedByChatId(City.ALMATA, chatId, PERIOD_START, PERIOD_END);

    assertTrue(found.isEmpty());
  }

  private OfficeAttendance saveAttendance(String chatId, City city, LocalDate date) {
    OfficeAttendance attendance = buildAttendance(chatId, city, date);
    repository.save(attendance);
    return attendance;
  }

  private static List<OfficeAttendance> ofChatIds(
      Collection<OfficeAttendance> attendances, String... chatIds) {
    Set<String> expectedChatIds = Set.of(chatIds);
    return attendances.stream().filter(a -> expectedChatIds.contains(a.getChatId())).toList();
  }

  private static OfficeAttendance buildAttendance(String chatId, City city, LocalDate date) {
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Repo Test");
    OfficeAttendance attendance = new OfficeAttendance();
    attendance.setChatId(chatId);
    attendance.setUsername("Repo Test");
    attendance.setCity(city);
    attendance.setWillCome(true);
    attendance.setDate(date.toString());
    return attendance;
  }
}
