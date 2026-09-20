/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.migration.OfficeAttendanceSourceScanner.SourceEntry;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.JdbcOfficeAttendanceRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfficeAttendancePostMigrationVerifier {

  private static final String ATTENDANCE_STORAGE_PATH = "attendance";
  private static final String FIELD_CITY = "city";
  private static final String FIELD_WILL_COME = "willCome";
  private static final String FIELD_DATE = "date";

  private OfficeAttendancePostMigrationVerifier() {}

  public static void main(String[] args) {
    Path attendanceBasePath = Path.of(BotConfig.getBotStorePath()).resolve(ATTENDANCE_STORAGE_PATH);
    JdbcOfficeAttendanceRepository postgresRepository =
        new JdbcOfficeAttendanceRepository(PersistenceConfig.getDataSource());

    OfficeAttendanceComparisonReport report = verify(attendanceBasePath, postgresRepository);

    log.info(
        "Source has [{}] record(s), destination has [{}] record(s)",
        report.getSourceCount(),
        report.getDestinationCount());
    report
        .getPerDateCountMismatches()
        .values()
        .forEach(mismatch -> log.warn("Count mismatch: {}", mismatch));
    report.getMissingIds().forEach(id -> log.warn("Missing in destination: {}", id));
    report.getExtraIds().forEach(id -> log.warn("Extra in destination: {}", id));
    report.getFieldMismatches().forEach(mismatch -> log.warn("Field mismatch: {}", mismatch));
    log.info("isMatch=[{}]", report.isMatch());
  }

  public static OfficeAttendanceComparisonReport verify(
      Path attendanceBasePath, JdbcOfficeAttendanceRepository postgresRepository) {
    List<SourceEntry> entries = OfficeAttendanceSourceScanner.scan(attendanceBasePath);
    Map<String, OfficeAttendance> source = resolveSourceRecords(entries);
    Map<String, OfficeAttendance> destination =
        postgresRepository.getAll().stream()
            .collect(Collectors.toMap(OfficeAttendance::getId, attendance -> attendance));

    Map<LocalDate, CountMismatch> perDateCountMismatches =
        findPerDateCountMismatches(source, destination);
    List<String> missingIds =
        source.keySet().stream().filter(id -> !destination.containsKey(id)).sorted().toList();
    List<String> extraIds =
        destination.keySet().stream().filter(id -> !source.containsKey(id)).sorted().toList();
    List<FieldMismatch> fieldMismatches = findFieldMismatches(source, destination);

    return new OfficeAttendanceComparisonReport(
        source.size(),
        destination.size(),
        perDateCountMismatches,
        missingIds,
        extraIds,
        fieldMismatches);
  }

  private static Map<String, OfficeAttendance> resolveSourceRecords(List<SourceEntry> entries) {
    Map<String, List<SourceEntry>> byEffectiveId =
        entries.stream().collect(Collectors.groupingBy(SourceEntry::effectiveId));
    Map<String, OfficeAttendance> resolved = new HashMap<>();
    for (Map.Entry<String, List<SourceEntry>> entry : byEffectiveId.entrySet()) {
      SourceEntry winner = OfficeAttendanceSourceScanner.pickWinner(entry.getValue());
      resolved.put(
          entry.getKey(),
          new OfficeAttendance(
              winner.getChatId(),
              null,
              winner.getCity(),
              winner.getWillCome(),
              winner.getEffectiveDate().toString()));
    }
    return resolved;
  }

  private static Map<LocalDate, CountMismatch> findPerDateCountMismatches(
      Map<String, OfficeAttendance> source, Map<String, OfficeAttendance> destination) {
    Map<LocalDate, Long> sourceCountsByDate =
        source.values().stream()
            .collect(
                Collectors.groupingBy(
                    attendance -> LocalDate.parse(attendance.getDate()), Collectors.counting()));
    Map<LocalDate, Long> destinationCountsByDate =
        destination.values().stream()
            .collect(
                Collectors.groupingBy(
                    attendance -> LocalDate.parse(attendance.getDate()), Collectors.counting()));

    Set<LocalDate> allDates = new HashSet<>(sourceCountsByDate.keySet());
    allDates.addAll(destinationCountsByDate.keySet());

    Map<LocalDate, CountMismatch> mismatches = new HashMap<>();
    for (LocalDate date : allDates) {
      long sourceCount = sourceCountsByDate.getOrDefault(date, 0L);
      long destinationCount = destinationCountsByDate.getOrDefault(date, 0L);
      if (sourceCount != destinationCount) {
        mismatches.put(date, new CountMismatch(date, (int) sourceCount, (int) destinationCount));
      }
    }
    return mismatches;
  }

  private static List<FieldMismatch> findFieldMismatches(
      Map<String, OfficeAttendance> source, Map<String, OfficeAttendance> destination) {
    List<FieldMismatch> mismatches = new ArrayList<>();
    for (Map.Entry<String, OfficeAttendance> entry : source.entrySet()) {
      OfficeAttendance sourceAttendance = entry.getValue();
      OfficeAttendance destinationAttendance = destination.get(entry.getKey());
      if (destinationAttendance == null) {
        continue;
      }
      compareField(
          entry.getKey(),
          FIELD_CITY,
          String.valueOf(sourceAttendance.getCity()),
          String.valueOf(destinationAttendance.getCity()),
          mismatches);
      compareField(
          entry.getKey(),
          FIELD_WILL_COME,
          String.valueOf(sourceAttendance.getWillCome()),
          String.valueOf(destinationAttendance.getWillCome()),
          mismatches);
      compareField(
          entry.getKey(),
          FIELD_DATE,
          sourceAttendance.getDate(),
          destinationAttendance.getDate(),
          mismatches);
    }
    return mismatches;
  }

  private static void compareField(
      String id,
      String field,
      String sourceValue,
      String destinationValue,
      List<FieldMismatch> mismatches) {
    if (!Objects.equals(sourceValue, destinationValue)) {
      mismatches.add(new FieldMismatch(id, field, sourceValue, destinationValue));
    }
  }
}
