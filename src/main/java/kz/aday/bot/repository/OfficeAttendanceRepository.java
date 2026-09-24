/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import javax.sql.DataSource;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Id;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.repository.schema.Columns;
import org.springframework.dao.support.DataAccessUtils;

public class OfficeAttendanceRepository extends AbstractRepository<OfficeAttendance> {

  private static final String SELECT_ALL =
      "SELECT oa.chat_id, u.prefered_name AS username, oa.city, oa.will_come, oa.date "
          + "FROM office_attendance oa "
          + "JOIN users u ON u.chat_id = oa.chat_id";
  private static final String SELECT_BY_ID = SELECT_ALL + " WHERE oa.chat_id = ? AND oa.date = ?";
  private static final String SELECT_BY_DATE = SELECT_ALL + " WHERE oa.date = ?";
  private static final String SELECT_ATTENDED_IN_PERIOD =
      SELECT_ALL + " WHERE oa.will_come = TRUE AND oa.city = ? AND oa.date BETWEEN ? AND ?";
  private static final String SELECT_ATTENDED_IN_PERIOD_BY_CHAT_ID =
      SELECT_ATTENDED_IN_PERIOD + " AND oa.chat_id = ?";
  private static final String UPSERT =
      "INSERT INTO office_attendance (chat_id, city, will_come, date) "
          + "VALUES (?, ?, ?, ?) "
          + "ON CONFLICT (chat_id, date) DO UPDATE SET "
          + "city = EXCLUDED.city, "
          + "will_come = EXCLUDED.will_come";
  private static final String DELETE_BY_ID =
      "DELETE FROM office_attendance WHERE chat_id = ? AND date = ?";

  public OfficeAttendanceRepository(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  public OfficeAttendance getById(String id, LocalDate date) {
    return DataAccessUtils.singleResult(
        jdbcTemplate.query(
            SELECT_BY_ID, OfficeAttendanceRepository::mapRow, toChatId(Id.ownerOf(id)), date));
  }

  @Override
  public Collection<OfficeAttendance> getAll(LocalDate date) {
    return jdbcTemplate.query(SELECT_BY_DATE, OfficeAttendanceRepository::mapRow, date);
  }

  @Override
  public Collection<OfficeAttendance> getAll() {
    return jdbcTemplate.query(SELECT_ALL, OfficeAttendanceRepository::mapRow);
  }

  public Collection<OfficeAttendance> findAttended(City city, LocalDate from, LocalDate to) {
    return jdbcTemplate.query(
        SELECT_ATTENDED_IN_PERIOD, OfficeAttendanceRepository::mapRow, enumName(city), from, to);
  }

  public Collection<OfficeAttendance> findAttendedByChatId(
      City city, String chatId, LocalDate from, LocalDate to) {
    return jdbcTemplate.query(
        SELECT_ATTENDED_IN_PERIOD_BY_CHAT_ID,
        OfficeAttendanceRepository::mapRow,
        enumName(city),
        from,
        to,
        toChatId(chatId));
  }

  @Override
  public void save(OfficeAttendance attendance) {
    jdbcTemplate.update(
        UPSERT,
        toChatId(attendance.getChatId()),
        enumName(attendance.getCity()),
        attendance.getWillCome(),
        attendance.getStorageDate());
    log.info("Saved office attendance [{}]", attendance.getId());
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    int deleted = jdbcTemplate.update(DELETE_BY_ID, toChatId(Id.ownerOf(id)), date);
    log.info("Deleted [{}] office attendance record(s) with id [{}]", deleted, id);
  }

  private static OfficeAttendance mapRow(ResultSet resultSet, int rowNum) throws SQLException {
    OfficeAttendance attendance = new OfficeAttendance();
    attendance.setChatId(chatIdValue(resultSet, Columns.CHAT_ID));
    attendance.setUsername(resultSet.getString(Columns.USERNAME));
    attendance.setCity(enumValue(resultSet, Columns.CITY, City.class));
    attendance.setWillCome(resultSet.getObject(Columns.WILL_COME, Boolean.class));
    attendance.setDate(resultSet.getObject(Columns.DATE, LocalDate.class).toString());
    return attendance;
  }
}
