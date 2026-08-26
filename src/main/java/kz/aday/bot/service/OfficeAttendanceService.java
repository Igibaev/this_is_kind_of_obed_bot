/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

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
}
