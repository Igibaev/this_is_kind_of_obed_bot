/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.JdbcOfficeAttendanceRepository;
import kz.aday.bot.repository.JsonFileStorageSupport;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OfficeAttendancePostMigrationVerifierTest extends AbstractDbPersistenceTest {

  private static final LocalDate SAFE_DATE = LocalDate.of(2099, 6, 15);
  private static final Path ATTENDANCE_BASE_PATH =
      Path.of(BotConfig.getBotStorePath()).resolve("attendance");
  private static final ObjectMapper OBJECT_MAPPER = JsonFileStorageSupport.createObjectMapper();

  private final JdbcOfficeAttendanceRepository postgresRepository =
      new JdbcOfficeAttendanceRepository(PersistenceConfig.getDataSource());
  private final List<Path> writtenFiles = new ArrayList<>();
  private final List<String> savedChatIds = new ArrayList<>();

  @AfterEach
  void cleanUp() throws IOException {
    for (Path file : writtenFiles) {
      Files.deleteIfExists(file);
    }
    writtenFiles.clear();
    for (String chatId : savedChatIds) {
      postgresRepository.deleteById(chatId + "_" + SAFE_DATE, SAFE_DATE);
    }
    savedChatIds.clear();
  }

  @Test
  void verify_reportsNoDiscrepancies_forARecordMigratedByTheRealMigrator() throws IOException {
    String chatId = "956000001";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Migrated");
    writeRawFile(chatId, attendance(chatId, City.ALMATA, true));
    savedChatIds.add(chatId);

    OfficeAttendanceJsonToPostgresMigrator.main(new String[0]);
    OfficeAttendanceComparisonReport report =
        OfficeAttendancePostMigrationVerifier.verify(ATTENDANCE_BASE_PATH, postgresRepository);

    assertFalse(report.getMissingIds().contains(id));
    assertFalse(report.getExtraIds().contains(id));
    assertTrue(report.getFieldMismatches().stream().noneMatch(m -> m.getId().equals(id)));
    assertFalse(report.getPerDateCountMismatches().containsKey(SAFE_DATE));
  }

  @Test
  void verify_reportsMissingIdAndCountMismatch_whenSourceRecordNeverReachedPostgres()
      throws IOException {
    String chatId = "956000002";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Unmigrated");
    writeRawFile(chatId, attendance(chatId, City.ALMATA, true));

    OfficeAttendanceComparisonReport report =
        OfficeAttendancePostMigrationVerifier.verify(ATTENDANCE_BASE_PATH, postgresRepository);

    assertTrue(report.getMissingIds().contains(id));
    CountMismatch mismatch = report.getPerDateCountMismatches().get(SAFE_DATE);
    assertEquals(1, mismatch.getSourceCount());
    assertEquals(0, mismatch.getDestinationCount());
  }

  @Test
  void verify_reportsExtraIdAndCountMismatch_whenDestinationHasNoMatchingSource() {
    String chatId = "956000003";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Extra");
    postgresRepository.save(
        new OfficeAttendance(chatId, null, City.ALMATA, true, SAFE_DATE.toString()));
    savedChatIds.add(chatId);

    OfficeAttendanceComparisonReport report =
        OfficeAttendancePostMigrationVerifier.verify(ATTENDANCE_BASE_PATH, postgresRepository);

    assertTrue(report.getExtraIds().contains(id));
    CountMismatch mismatch = report.getPerDateCountMismatches().get(SAFE_DATE);
    assertEquals(0, mismatch.getSourceCount());
    assertEquals(1, mismatch.getDestinationCount());
  }

  @Test
  void verify_reportsFieldMismatches_whenCityAndWillComeDifferFromDestination() throws IOException {
    String chatId = "956000004";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Diverged");
    writeRawFile(chatId, attendance(chatId, City.ALMATA, true));
    postgresRepository.save(
        new OfficeAttendance(chatId, null, City.ASTANA, false, SAFE_DATE.toString()));
    savedChatIds.add(chatId);

    OfficeAttendanceComparisonReport report =
        OfficeAttendancePostMigrationVerifier.verify(ATTENDANCE_BASE_PATH, postgresRepository);

    FieldMismatch cityMismatch = fieldMismatch(report, id, "city");
    assertEquals("ALMATA", cityMismatch.getSourceValue());
    assertEquals("ASTANA", cityMismatch.getDestinationValue());
    FieldMismatch willComeMismatch = fieldMismatch(report, id, "willCome");
    assertEquals("true", willComeMismatch.getSourceValue());
    assertEquals("false", willComeMismatch.getDestinationValue());
  }

  @Test
  void verify_reportsFieldMismatch_whenWillComeIsNullInSourceButFalseInDestination()
      throws IOException {
    String chatId = "956000005";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "NullSource");
    OfficeAttendance sourceAttendance = attendance(chatId, City.ALMATA, true);
    sourceAttendance.setWillCome(null);
    writeRawFile(chatId, sourceAttendance);
    postgresRepository.save(
        new OfficeAttendance(chatId, null, City.ALMATA, false, SAFE_DATE.toString()));
    savedChatIds.add(chatId);

    OfficeAttendanceComparisonReport report =
        OfficeAttendancePostMigrationVerifier.verify(ATTENDANCE_BASE_PATH, postgresRepository);

    FieldMismatch willComeMismatch = fieldMismatch(report, id, "willCome");
    assertEquals("null", willComeMismatch.getSourceValue());
    assertEquals("false", willComeMismatch.getDestinationValue());
  }

  @Test
  void verify_doesNotReportUsernameDifferences_sinceUsernameIsNotPersisted() throws IOException {
    String chatId = "956000006";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(
        PersistenceConfig.getDataSource(), Long.parseLong(chatId), "DestinationName");
    OfficeAttendance sourceAttendance = attendance(chatId, City.ALMATA, true);
    sourceAttendance.setUsername("SourceName");
    writeRawFile(chatId, sourceAttendance);
    postgresRepository.save(
        new OfficeAttendance(chatId, null, City.ALMATA, true, SAFE_DATE.toString()));
    savedChatIds.add(chatId);

    OfficeAttendanceComparisonReport report =
        OfficeAttendancePostMigrationVerifier.verify(ATTENDANCE_BASE_PATH, postgresRepository);

    assertTrue(report.getFieldMismatches().stream().noneMatch(m -> m.getId().equals(id)));
  }

  private static FieldMismatch fieldMismatch(
      OfficeAttendanceComparisonReport report, String id, String field) {
    return report.getFieldMismatches().stream()
        .filter(m -> m.getId().equals(id) && m.getField().equals(field))
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("Expected a [" + field + "] mismatch for id [" + id + "]"));
  }

  private void writeRawFile(String chatId, OfficeAttendance attendance) throws IOException {
    Path folder = ATTENDANCE_BASE_PATH.resolve(SAFE_DATE.toString());
    Files.createDirectories(folder);
    Path file = folder.resolve(chatId + ".json");
    OBJECT_MAPPER.writeValue(file.toFile(), attendance);
    writtenFiles.add(file);
  }

  private static OfficeAttendance attendance(String chatId, City city, boolean willCome) {
    OfficeAttendance attendance = new OfficeAttendance();
    attendance.setChatId(chatId);
    attendance.setCity(city);
    attendance.setWillCome(willCome);
    attendance.setDate(SAFE_DATE.toString());
    return attendance;
  }
}
