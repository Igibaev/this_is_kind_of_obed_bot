/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JdbcOfficeAttendanceRepository implements Repository<OfficeAttendance> {

  private static final String SELECT_ALL =
      "SELECT chat_id, username, city, will_come, date FROM office_attendance";
  private static final String SELECT_BY_ID = SELECT_ALL + " WHERE chat_id = ? AND date = ?";
  private static final String SELECT_BY_DATE = SELECT_ALL + " WHERE date = ?";
  private static final String UPSERT =
      "INSERT INTO office_attendance (chat_id, username, city, will_come, date) "
          + "VALUES (?, ?, ?, ?, ?) "
          + "ON CONFLICT (chat_id, date) DO UPDATE SET "
          + "username = EXCLUDED.username, "
          + "city = EXCLUDED.city, "
          + "will_come = EXCLUDED.will_come";
  private static final String DELETE_BY_ID =
      "DELETE FROM office_attendance WHERE chat_id = ? AND date = ?";

  private final DataSource dataSource;

  public JdbcOfficeAttendanceRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public OfficeAttendance getById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setLong(1, extractChatId(id));
      statement.setObject(2, date);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapRow(resultSet) : null;
      }
    } catch (SQLException e) {
      log.error("Failure loading office attendance [{}]", id, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean existById(String id, LocalDate date) {
    return getById(id, date) != null;
  }

  @Override
  public Collection<OfficeAttendance> getAll(LocalDate date) {
    List<OfficeAttendance> attendances = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_DATE)) {
      statement.setObject(1, date);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          attendances.add(mapRow(resultSet));
        }
      }
      return attendances;
    } catch (SQLException e) {
      log.error("Failure loading office attendances for date [{}]", date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<OfficeAttendance> getAll() {
    List<OfficeAttendance> attendances = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        attendances.add(mapRow(resultSet));
      }
      return attendances;
    } catch (SQLException e) {
      log.error("Failure loading office attendances", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(OfficeAttendance attendance) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPSERT)) {
      statement.setLong(1, Long.parseLong(attendance.getChatId()));
      statement.setString(2, attendance.getUsername());
      statement.setString(3, attendance.getCity() != null ? attendance.getCity().name() : null);
      if (attendance.getWillCome() != null) {
        statement.setBoolean(4, attendance.getWillCome());
      } else {
        statement.setNull(4, Types.BOOLEAN);
      }
      statement.setObject(5, attendance.getStorageDate());
      statement.executeUpdate();
      log.info("Saved office attendance [{}]", attendance.getId());
    } catch (SQLException e) {
      log.error("Failure saving office attendance [{}]", attendance.getId(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearLastWeek() {
    log.info("Clearing office attendance storage is skiped");
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID)) {
      statement.setLong(1, extractChatId(id));
      statement.setObject(2, date);
      int deleted = statement.executeUpdate();
      log.info("Deleted [{}] office attendance record(s) with id [{}]", deleted, id);
    } catch (SQLException e) {
      log.error("Failure deleting office attendance [{}]", id, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearStorage() {
    log.info("Clearing office attendance storage is skiped");
  }

  private static long extractChatId(String id) {
    return Long.parseLong(id.substring(0, id.indexOf('_')));
  }

  private OfficeAttendance mapRow(ResultSet resultSet) throws SQLException {
    OfficeAttendance attendance = new OfficeAttendance();
    attendance.setChatId(String.valueOf(resultSet.getLong("chat_id")));
    attendance.setUsername(resultSet.getString("username"));
    attendance.setCity(JdbcMappingSupport.mapEnum(resultSet.getString("city"), City::valueOf));
    boolean willCome = resultSet.getBoolean("will_come");
    attendance.setWillCome(resultSet.wasNull() ? null : willCome);
    attendance.setDate(resultSet.getObject("date", LocalDate.class).toString());
    return attendance;
  }
}
