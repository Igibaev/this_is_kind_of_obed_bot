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
import java.util.function.Function;
import javax.sql.DataSource;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JdbcUserRepository implements Repository<User> {

  private static final String SELECT_ALL =
      "SELECT chat_id, prefered_name, last_message_id, city, role, state, status FROM users";
  private static final String SELECT_BY_ID = SELECT_ALL + " WHERE chat_id = ?";
  private static final String UPSERT =
      "INSERT INTO users (chat_id, prefered_name, last_message_id, city, role, state, status) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?) "
          + "ON CONFLICT (chat_id) DO UPDATE SET "
          + "prefered_name = EXCLUDED.prefered_name, "
          + "last_message_id = EXCLUDED.last_message_id, "
          + "city = EXCLUDED.city, "
          + "role = EXCLUDED.role, "
          + "state = EXCLUDED.state, "
          + "status = EXCLUDED.status";
  private static final String DELETE_BY_ID = "DELETE FROM users WHERE chat_id = ?";

  private final DataSource dataSource;

  public JdbcUserRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public User getById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
      statement.setLong(1, Long.parseLong(id));
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapRow(resultSet) : null;
      }
    } catch (SQLException e) {
      log.error("Failure loading user [{}]", id, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean existById(String id, LocalDate date) {
    return getById(id, date) != null;
  }

  @Override
  public Collection<User> getAll(LocalDate date) {
    return getAll();
  }

  @Override
  public Collection<User> getAll() {
    List<User> users = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        users.add(mapRow(resultSet));
      }
      return users;
    } catch (SQLException e) {
      log.error("Failure loading users", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(User user) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPSERT)) {
      statement.setLong(1, user.getChatId());
      statement.setString(2, user.getPreferedName());
      if (user.getLastMessageId() != null) {
        statement.setInt(3, user.getLastMessageId());
      } else {
        statement.setNull(3, Types.INTEGER);
      }
      statement.setString(4, user.getCity() != null ? user.getCity().name() : null);
      statement.setString(5, user.getRole() != null ? user.getRole().name() : null);
      statement.setString(6, user.getState() != null ? user.getState().name() : null);
      statement.setString(7, user.getStatus() != null ? user.getStatus().name() : null);
      statement.executeUpdate();
      log.info("Saved user [{}]", user.getId());
    } catch (SQLException e) {
      log.error("Failure saving user [{}]", user.getId(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearLastWeek() {
    log.info("Clearing user storage is skiped");
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID)) {
      statement.setLong(1, Long.parseLong(id));
      int deleted = statement.executeUpdate();
      log.info("Deleted [{}] user(s) with id [{}]", deleted, id);
    } catch (SQLException e) {
      log.error("Failure deleting user [{}]", id, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearStorage() {
    log.info("Clearing user storage is skiped");
  }

  private User mapRow(ResultSet resultSet) throws SQLException {
    User user = new User();
    user.setChatId(resultSet.getLong("chat_id"));
    user.setPreferedName(resultSet.getString("prefered_name"));
    int lastMessageId = resultSet.getInt("last_message_id");
    user.setLastMessageId(resultSet.wasNull() ? null : lastMessageId);
    user.setCity(mapEnum(resultSet.getString("city"), City::valueOf));
    user.setRole(mapEnum(resultSet.getString("role"), User.Role::valueOf));
    user.setState(mapEnum(resultSet.getString("state"), State::valueOf));
    user.setStatus(mapEnum(resultSet.getString("status"), Status::valueOf));
    return user;
  }

  private <T> T mapEnum(String value, Function<String, T> parser) {
    return value == null ? null : parser.apply(value);
  }
}
