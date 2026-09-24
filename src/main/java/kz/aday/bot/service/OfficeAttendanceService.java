/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.JdbcOfficeAttendanceRepository;
import kz.aday.bot.repository.Repository;
import kz.aday.bot.util.StringUtils;

public class OfficeAttendanceService {
  static final String NO_DATA_MESSAGE = "Нет данных о посещениях.";
  private static final String USER_STATS_TEMPLATE = "%s: %d";
  private static final String STATS_LINE_DELIMITER = "\n";
  private static final int DAYS_UNTIL_DEFAULT_ATTENDANCE_DATE = 1;

  private final Repository<OfficeAttendance> repository;

  public OfficeAttendanceService() {
    this(new JdbcOfficeAttendanceRepository(PersistenceConfig.getDataSource()));
  }

  OfficeAttendanceService(Repository<OfficeAttendance> repository) {
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
    return collectStats(city, attendance -> true);
  }

  public String getCurrentMonthAttendanceStats(City city) {
    return collectStats(city, inCurrentMonth());
  }

  public String getOverallAttendanceStatsForUser(City city, String userId) {
    return collectStats(city, ofUser(userId));
  }

  public String getCurrentMonthAttendanceStatsForUser(City city, String userId) {
    return collectStats(city, ofUser(userId).and(inCurrentMonth()));
  }

  private String collectStats(City city, Predicate<OfficeAttendance> extraFilter) {
    LocalDate today = LocalDate.now();
    List<OfficeAttendance> attendances =
        repository.getAll().stream()
            .filter(a -> Boolean.TRUE.equals(a.getWillCome()))
            .filter(a -> city.equals(a.getCity()))
            .filter(a -> parseDate(a).filter(date -> !date.isAfter(today)).isPresent())
            .filter(extraFilter)
            .toList();
    return formatStats(attendances);
  }

  private static Predicate<OfficeAttendance> ofUser(String userId) {
    return attendance -> userId.equals(attendance.getChatId());
  }

  private static Predicate<OfficeAttendance> inCurrentMonth() {
    YearMonth currentMonth = YearMonth.now();
    return attendance ->
        parseDate(attendance).map(YearMonth::from).filter(currentMonth::equals).isPresent();
  }

  private static Optional<LocalDate> parseDate(OfficeAttendance attendance) {
    if (attendance.getDate() == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(attendance.getDate()));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  private static String formatStats(List<OfficeAttendance> attendances) {
    if (attendances.isEmpty()) {
      return NO_DATA_MESSAGE;
    }
    return attendances.stream()
        .collect(Collectors.groupingBy(OfficeAttendance::getChatId))
        .values()
        .stream()
        .sorted(Comparator.<List<OfficeAttendance>>comparingInt(List::size).reversed())
        .map(
            list ->
                String.format(
                    USER_STATS_TEMPLATE,
                    StringUtils.escapeMarkdown(list.get(0).getUsername()),
                    list.size()))
        .collect(Collectors.joining(STATS_LINE_DELIMITER));
  }
}
