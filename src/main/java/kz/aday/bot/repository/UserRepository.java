/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import javax.sql.DataSource;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.schema.Columns;
import org.springframework.dao.support.DataAccessUtils;

public class UserRepository extends AbstractRepository<User> {

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

  public UserRepository(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  public User getById(String id, LocalDate date) {
    return DataAccessUtils.singleResult(
        jdbcTemplate.query(SELECT_BY_ID, UserRepository::mapRow, toChatId(id)));
  }

  @Override
  public Collection<User> getAll(LocalDate date) {
    return getAll();
  }

  @Override
  public Collection<User> getAll() {
    return jdbcTemplate.query(SELECT_ALL, UserRepository::mapRow);
  }

  @Override
  public void save(User user) {
    jdbcTemplate.update(
        UPSERT,
        user.getChatId(),
        user.getPreferedName(),
        user.getLastMessageId(),
        enumName(user.getCity()),
        enumName(user.getRole()),
        enumName(user.getState()),
        enumName(user.getStatus()));
    log.info("Saved user [{}]", user.getId());
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    int deleted = jdbcTemplate.update(DELETE_BY_ID, toChatId(id));
    log.info("Deleted [{}] user(s) with id [{}]", deleted, id);
  }

  private static User mapRow(ResultSet resultSet, int rowNum) throws SQLException {
    User user = new User();
    user.setChatId(resultSet.getLong(Columns.CHAT_ID));
    user.setPreferedName(resultSet.getString(Columns.PREFERED_NAME));
    user.setLastMessageId(resultSet.getObject(Columns.LAST_MESSAGE_ID, Integer.class));
    user.setCity(enumValue(resultSet, Columns.CITY, City.class));
    user.setRole(enumValue(resultSet, Columns.ROLE, User.Role.class));
    user.setState(enumValue(resultSet, Columns.STATE, State.class));
    user.setStatus(enumValue(resultSet, Columns.STATUS, Status.class));
    return user;
  }
}
