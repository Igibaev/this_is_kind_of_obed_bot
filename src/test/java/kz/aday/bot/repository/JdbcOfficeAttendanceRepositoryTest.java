/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Collection;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.Test;

class JdbcOfficeAttendanceRepositoryTest extends AbstractDbPersistenceTest {

  private static final LocalDate TODAY = LocalDate.now();
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);

  private final JdbcOfficeAttendanceRepository repository =
      new JdbcOfficeAttendanceRepository(PersistenceConfig.getDataSource());

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
