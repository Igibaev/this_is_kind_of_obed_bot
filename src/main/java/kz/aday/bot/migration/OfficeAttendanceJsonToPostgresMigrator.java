/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.JdbcOfficeAttendanceRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfficeAttendanceJsonToPostgresMigrator {

  private static final String ATTENDANCE_STORAGE_PATH = "attendance";

  private OfficeAttendanceJsonToPostgresMigrator() {}

  public static void main(String[] args) {
    BaseRepository<OfficeAttendance> jsonRepository =
        new BaseRepository<>(
            new ConcurrentHashMap<>(), OfficeAttendance.class, ATTENDANCE_STORAGE_PATH);
    JdbcOfficeAttendanceRepository postgresRepository =
        new JdbcOfficeAttendanceRepository(PersistenceConfig.getDataSource());

    Collection<OfficeAttendance> attendances = jsonRepository.getAll();
    log.info(
        "Migrating [{}] office attendance record(s) from JSON storage to Postgres",
        attendances.size());

    for (OfficeAttendance attendance : attendances) {
      postgresRepository.save(attendance);
    }

    log.info("Migration complete");
  }
}
