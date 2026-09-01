/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.BaseRepository;

public class OfficeAttendanceService extends BaseService<OfficeAttendance> {

  public OfficeAttendanceService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), OfficeAttendance.class, "attendance"));
  }

  public void save(String userId, String username, City city, boolean willCome) {
    save(userId, username, city, willCome, LocalDate.now().plusDays(1));
  }

  public void save(String userId, String username, City city, boolean willCome, LocalDate date) {
    OfficeAttendance officeAttendance = new OfficeAttendance();
    officeAttendance.setChatId(userId);
    officeAttendance.setUsername(username);
    officeAttendance.setCity(city);
    officeAttendance.setWillCome(willCome);
    officeAttendance.setDate(date.toString());
    save(officeAttendance);
  }

  public String getOverallAttendanceStats(City city) {
    List<OfficeAttendance> attendances =
        repository.getAll().stream()
            .filter(a -> Boolean.TRUE.equals(a.getWillCome()))
            .filter(a -> city.equals(a.getCity()))
            .toList();
    return formatStats(attendances);
  }

  public String getCurrentMonthAttendanceStats(City city) {
    YearMonth currentMonth = YearMonth.now();
    List<OfficeAttendance> attendances =
        repository.getAll().stream()
            .filter(a -> Boolean.TRUE.equals(a.getWillCome()))
            .filter(a -> city.equals(a.getCity()))
            .filter(a -> isInMonth(a, currentMonth))
            .toList();
    return formatStats(attendances);
  }

  private boolean isInMonth(OfficeAttendance attendance, YearMonth month) {
    if (attendance.getDate() == null) {
      return false;
    }
    try {
      return YearMonth.from(LocalDate.parse(attendance.getDate())).equals(month);
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private String formatStats(List<OfficeAttendance> attendances) {
    if (attendances.isEmpty()) {
      return "Нет данных о посещениях.";
    }
    Map<String, List<OfficeAttendance>> byChatId =
        attendances.stream().collect(Collectors.groupingBy(OfficeAttendance::getChatId));
    return byChatId.values().stream()
        .sorted(Comparator.<List<OfficeAttendance>>comparingInt(List::size).reversed())
        .map(list -> String.format("%s: %d", list.get(0).getUsername(), list.size()))
        .collect(Collectors.joining("\n"));
  }
}
