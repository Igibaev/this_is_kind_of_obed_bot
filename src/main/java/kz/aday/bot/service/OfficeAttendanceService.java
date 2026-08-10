/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.BaseRepository;

public class OfficeAttendanceService extends BaseService<OfficeAttendance> {

  public OfficeAttendanceService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), OfficeAttendance.class, "attendance"));
  }

  public void save(String userId, String username, City city, boolean willCome) {
    OfficeAttendance officeAttendance = new OfficeAttendance();
    officeAttendance.setChatId(userId);
    officeAttendance.setUsername(username);
    officeAttendance.setCity(city);
    officeAttendance.setWillCome(willCome);
    save(officeAttendance);
  }
}
