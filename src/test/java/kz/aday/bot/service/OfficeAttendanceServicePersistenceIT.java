/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.Test;

class OfficeAttendanceServicePersistenceIT extends AbstractDbPersistenceTest {

  private static final LocalDate TODAY = LocalDate.now();

  @Test
  void save_thenFindByChatId_returnsPersistedAttendance() {
    String chatId = "930000001";
    OfficeAttendance expected = buildAttendance(chatId, City.ALMATA);
    OfficeAttendanceService service = new OfficeAttendanceService();

    service.save(chatId, City.ALMATA, true, TODAY);
    OfficeAttendance found = service.findByChatId(chatId, TODAY);

    assertEquals(expected, found);
    assertEquals(TODAY, found.getStorageDate());
  }

  @Test
  void newServiceInstance_seesAttendanceSavedByPreviousInstance() {
    String chatId = "930000002";
    OfficeAttendance expected = buildAttendance(chatId, City.ASTANA);
    new OfficeAttendanceService().save(chatId, City.ASTANA, true, TODAY);

    OfficeAttendance found = new OfficeAttendanceService().findByChatId(chatId, TODAY);

    assertEquals(expected, found);
  }

  @Test
  void findByChatId_returnsNull_whenNoAttendanceOnDate() {
    String chatId = "930000003";
    buildAttendance(chatId, City.ALMATA);
    OfficeAttendanceService service = new OfficeAttendanceService();
    service.save(chatId, City.ALMATA, true, TODAY);

    assertNull(service.findByChatId(chatId, TODAY.minusDays(1)));
  }

  private static OfficeAttendance buildAttendance(String chatId, City city) {
    TestUsers.ensureExists(
        PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Persist User");
    OfficeAttendance attendance = new OfficeAttendance();
    attendance.setChatId(chatId);
    attendance.setUsername("Persist User");
    attendance.setCity(city);
    attendance.setWillCome(true);
    attendance.setDate(TODAY.toString());
    return attendance;
  }
}
