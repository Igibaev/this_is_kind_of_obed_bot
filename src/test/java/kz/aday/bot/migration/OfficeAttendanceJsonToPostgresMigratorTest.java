/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.JdbcOfficeAttendanceRepository;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OfficeAttendanceJsonToPostgresMigratorTest extends AbstractDbPersistenceTest {

  private static final LocalDate TODAY = LocalDate.now();
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);
  private static final String FIRST_CHAT_ID = "950000010";
  private static final String SECOND_CHAT_ID = "950000011";

  private final BaseRepository<OfficeAttendance> jsonRepository =
      new BaseRepository<>(new ConcurrentHashMap<>(), OfficeAttendance.class, "attendance");
  private final JdbcOfficeAttendanceRepository postgresRepository =
      new JdbcOfficeAttendanceRepository(PersistenceConfig.getDataSource());

  @AfterEach
  void cleanUpMigratedAttendances() {
    jsonRepository.deleteById(FIRST_CHAT_ID + "_" + TODAY, TODAY);
    jsonRepository.deleteById(SECOND_CHAT_ID + "_" + YESTERDAY, YESTERDAY);
    postgresRepository.deleteById(FIRST_CHAT_ID + "_" + TODAY, TODAY);
    postgresRepository.deleteById(SECOND_CHAT_ID + "_" + YESTERDAY, YESTERDAY);
  }

  @Test
  void main_migratesEveryAttendanceFromJsonStorageIntoPostgres_acrossDifferentDateFolders() {
    OfficeAttendance first = buildAttendance(FIRST_CHAT_ID, City.ALMATA, TODAY);
    OfficeAttendance second = buildAttendance(SECOND_CHAT_ID, City.ASTANA, YESTERDAY);
    jsonRepository.save(first);
    jsonRepository.save(second);

    OfficeAttendanceJsonToPostgresMigrator.main(new String[0]);

    OfficeAttendance migratedFirst = postgresRepository.getById(first.getId(), TODAY);
    OfficeAttendance migratedSecond = postgresRepository.getById(second.getId(), YESTERDAY);

    assertEquals(City.ALMATA, migratedFirst.getCity());
    assertEquals(City.ASTANA, migratedSecond.getCity());
  }

  @Test
  void main_overwritesExistingRow_whenAttendanceWasAlreadyMigratedBefore() {
    OfficeAttendance attendance = buildAttendance(FIRST_CHAT_ID, City.ALMATA, TODAY);
    jsonRepository.save(attendance);
    OfficeAttendance stale = buildAttendance(FIRST_CHAT_ID, City.ASTANA, TODAY);
    stale.setWillCome(false);
    postgresRepository.save(stale);

    OfficeAttendanceJsonToPostgresMigrator.main(new String[0]);

    OfficeAttendance migrated = postgresRepository.getById(attendance.getId(), TODAY);
    assertEquals(City.ALMATA, migrated.getCity());
    assertEquals(true, migrated.getWillCome());
  }

  @Test
  void main_doesNothing_whenNoJsonAttendancesExist() {
    OfficeAttendanceJsonToPostgresMigrator.main(new String[0]);

    assertNull(postgresRepository.getById(FIRST_CHAT_ID + "_" + TODAY, TODAY));
  }

  private static OfficeAttendance buildAttendance(String chatId, City city, LocalDate date) {
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId));
    OfficeAttendance attendance = new OfficeAttendance();
    attendance.setChatId(chatId);
    attendance.setUsername("Migrator Test");
    attendance.setCity(city);
    attendance.setWillCome(true);
    attendance.setDate(date.toString());
    return attendance;
  }
}
