/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.testsupport.AbstractPersistenceTest;
import org.junit.jupiter.api.Test;

class OfficeAttendanceServicePersistenceIT extends AbstractPersistenceTest {

  private static final LocalDate TODAY = LocalDate.now();

  @Test
  void save_thenFindById_returnsPersistedAttendance() {
    String chatId = "930000001";
    OfficeAttendance expected = buildAttendance(chatId, City.ALMATA);
    OfficeAttendanceService service = new OfficeAttendanceService();

    service.save(chatId, expected.getUsername(), City.ALMATA, true, TODAY);
    OfficeAttendance found = service.findById(expected.getId());

    assertEquals(expected, found);
    assertEquals(TODAY, found.getStorageDate());
  }

  @Test
  void newServiceInstance_seesAttendanceSavedByPreviousInstance() {
    String chatId = "930000002";
    OfficeAttendance expected = buildAttendance(chatId, City.ASTANA);
    new OfficeAttendanceService().save(chatId, expected.getUsername(), City.ASTANA, true, TODAY);

    OfficeAttendance found = new OfficeAttendanceService().findById(expected.getId());

    assertEquals(expected, found);
  }

  @Test
  void deleteById_removesAttendanceFromStorage() {
    String chatId = "930000003";
    OfficeAttendance expected = buildAttendance(chatId, City.ALMATA);
    OfficeAttendanceService service = new OfficeAttendanceService();
    service.save(chatId, expected.getUsername(), City.ALMATA, true, TODAY);
    assertTrue(service.existsById(expected.getId()));

    service.deleteById(expected.getId());

    assertFalse(service.existsById(expected.getId()));
  }

  private static OfficeAttendance buildAttendance(String chatId, City city) {
    OfficeAttendance attendance = new OfficeAttendance();
    attendance.setChatId(chatId);
    attendance.setUsername("Persist User");
    attendance.setCity(city);
    attendance.setWillCome(true);
    attendance.setDate(TODAY.toString());
    return attendance;
  }
}
