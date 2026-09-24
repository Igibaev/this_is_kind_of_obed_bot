/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.AttendanceStat;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.OfficeAttendanceRepository;
import kz.aday.bot.util.StringUtils;

public class OfficeAttendanceService {
  static final String NO_DATA_MESSAGE = "Нет данных о посещениях.";
  private static final String USER_STATS_TEMPLATE = "%s: %d";
  private static final String STATS_LINE_DELIMITER = "\n";
  private static final int DAYS_UNTIL_DEFAULT_ATTENDANCE_DATE = 1;
  private static final int FIRST_DAY_OF_MONTH = 1;
  static final LocalDate OVERALL_STATS_START = LocalDate.EPOCH;

  private final OfficeAttendanceRepository repository;

  public OfficeAttendanceService() {
    this(new OfficeAttendanceRepository(PersistenceConfig.getDataSource()));
  }

  OfficeAttendanceService(OfficeAttendanceRepository repository) {
    this.repository = repository;
  }

  public OfficeAttendance findByChatId(String chatId, LocalDate date) {
    return repository.getById(OfficeAttendance.buildId(chatId, date), date);
  }

  public void save(String userId, City city, boolean willCome) {
    save(userId, city, willCome, LocalDate.now().plusDays(DAYS_UNTIL_DEFAULT_ATTENDANCE_DATE));
  }

  public void save(String userId, City city, boolean willCome, LocalDate date) {
    OfficeAttendance officeAttendance = new OfficeAttendance();
    officeAttendance.setChatId(userId);
    officeAttendance.setCity(city);
    officeAttendance.setWillCome(willCome);
    officeAttendance.setDate(date.toString());
    repository.save(officeAttendance);
  }

  public String getOverallAttendanceStats(City city) {
    return formatStats(
        merge(
            repository.findLeaderboard(city),
            countByUser(repository.findAttended(city, OVERALL_STATS_START, LocalDate.now()))));
  }

  public String getCurrentMonthAttendanceStats(City city) {
    return formatStats(
        countByUser(repository.findAttended(city, currentMonthStart(), LocalDate.now())));
  }

  public String getOverallAttendanceStatsForUser(City city, String userId) {
    return formatStats(
        merge(
            repository.findLeaderboardByChatId(city, userId),
            countByUser(
                repository.findAttendedByChatId(
                    city, userId, OVERALL_STATS_START, LocalDate.now()))));
  }

  public String getCurrentMonthAttendanceStatsForUser(City city, String userId) {
    return formatStats(
        countByUser(
            repository.findAttendedByChatId(city, userId, currentMonthStart(), LocalDate.now())));
  }

  public void consolidatePastMonths() {
    repository.consolidateAttendedBefore(currentMonthStart());
  }

  private static LocalDate currentMonthStart() {
    return YearMonth.now().atDay(FIRST_DAY_OF_MONTH);
  }

  private static Collection<AttendanceStat> countByUser(Collection<OfficeAttendance> attendances) {
    return attendances.stream()
        .collect(Collectors.groupingBy(OfficeAttendance::getChatId))
        .values()
        .stream()
        .map(
            list ->
                new AttendanceStat(list.get(0).getChatId(), list.get(0).getUsername(), list.size()))
        .toList();
  }

  private static Collection<AttendanceStat> merge(
      Collection<AttendanceStat> first, Collection<AttendanceStat> second) {
    return Stream.concat(first.stream(), second.stream())
        .collect(
            Collectors.toMap(
                AttendanceStat::getChatId,
                Function.identity(),
                (left, right) ->
                    new AttendanceStat(
                        left.getChatId(), left.getUsername(), left.getVisits() + right.getVisits()),
                LinkedHashMap::new))
        .values();
  }

  private static String formatStats(Collection<AttendanceStat> stats) {
    if (stats.isEmpty()) {
      return NO_DATA_MESSAGE;
    }
    return stats.stream()
        .sorted(Comparator.comparingInt(AttendanceStat::getVisits).reversed())
        .map(
            stat ->
                String.format(
                    USER_STATS_TEMPLATE,
                    StringUtils.escapeMarkdown(stat.getUsername()),
                    stat.getVisits()))
        .collect(Collectors.joining(STATS_LINE_DELIMITER));
  }
}
