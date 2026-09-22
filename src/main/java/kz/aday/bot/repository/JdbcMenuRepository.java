/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JdbcMenuRepository implements Repository<Menu> {

  private static final String SELECT_MENU_HEADER =
      "SELECT id, status, deadline, available, notificated, message "
          + "FROM menus WHERE city = ? AND date = ?";
  private static final String SELECT_ALL_MENUS =
      "SELECT id, city, date, status, deadline, available, notificated, message FROM menus";
  private static final String SELECT_MENUS_BY_DATE = SELECT_ALL_MENUS + " WHERE date = ?";
  private static final String SELECT_ITEMS_BY_MENU_ID =
      "SELECT display_order, name, category FROM menu_items "
          + "WHERE menu_id = ? ORDER BY display_order ASC";
  private static final String UPSERT_MENU =
      "INSERT INTO menus (city, date, status, deadline, available, notificated, message) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?) "
          + "ON CONFLICT (city, date) DO UPDATE SET "
          + "status = EXCLUDED.status, "
          + "deadline = EXCLUDED.deadline, "
          + "available = EXCLUDED.available, "
          + "notificated = EXCLUDED.notificated, "
          + "message = EXCLUDED.message "
          + "RETURNING id";
  private static final String SELECT_EXISTING_ITEM_IDS_BY_MENU_ID =
      "SELECT item_id, name FROM menu_items WHERE menu_id = ?";
  private static final String DELETE_ITEM_BY_ID = "DELETE FROM menu_items WHERE item_id = ?";
  private static final String SHIFT_DISPLAY_ORDER_TO_TEMP =
      "UPDATE menu_items SET display_order = -item_id WHERE item_id = ?";
  private static final String UPDATE_ITEM =
      "UPDATE menu_items SET display_order = ?, category = ? WHERE item_id = ?";
  private static final String INSERT_ITEM =
      "INSERT INTO menu_items (menu_id, display_order, name, category) VALUES (?, ?, ?, ?)";
  private static final String DELETE_MENU_BY_CITY_AND_DATE =
      "DELETE FROM menus WHERE city = ? AND date = ?";

  private final DataSource dataSource;

  public JdbcMenuRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Menu getById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection()) {
      return getById(connection, id, date);
    } catch (SQLException e) {
      log.error("Failure loading menu [{}] for date [{}]", id, date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean existById(String id, LocalDate date) {
    return getById(id, date) != null;
  }

  @Override
  public Collection<Menu> getAll(LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_MENUS_BY_DATE)) {
      statement.setObject(1, date);
      return loadMenus(connection, statement);
    } catch (SQLException e) {
      log.error("Failure loading menus for date [{}]", date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<Menu> getAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL_MENUS)) {
      return loadMenus(connection, statement);
    } catch (SQLException e) {
      log.error("Failure loading menus", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(Menu menu) {
    LocalDate date = menu.getStorageDate();
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        long menuId = upsertMenu(connection, menu, date);
        replaceItems(connection, menuId, menu.getItemList());
        connection.commit();
        log.info("Saved menu [{}] for date [{}]", menu.getId(), date);
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      log.error("Failure saving menu [{}] for date [{}]", menu.getId(), date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearLastWeek() {
    log.info("Clearing menu storage is skipped");
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_MENU_BY_CITY_AND_DATE)) {
      statement.setString(1, id);
      statement.setObject(2, date);
      int deleted = statement.executeUpdate();
      log.info("Deleted [{}] menu(s) with id [{}] for date [{}]", deleted, id, date);
    } catch (SQLException e) {
      log.error("Failure deleting menu [{}] for date [{}]", id, date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearStorage() {
    log.info("Clearing menu storage is skipped");
  }

  private Menu getById(Connection connection, String id, LocalDate date) throws SQLException {
    Menu menu;
    long menuId;
    try (PreparedStatement statement = connection.prepareStatement(SELECT_MENU_HEADER)) {
      statement.setString(1, id);
      statement.setObject(2, date);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        menu = mapHeaderRow(resultSet, City.valueOf(id), date);
        menuId = resultSet.getLong("id");
      }
    }
    menu.setItemList(loadItems(connection, menuId));
    return menu;
  }

  private List<Menu> loadMenus(Connection connection, PreparedStatement statement)
      throws SQLException {
    List<Menu> menus = new ArrayList<>();
    List<Long> menuIds = new ArrayList<>();
    try (ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        City city = JdbcMappingSupport.mapEnum(resultSet.getString("city"), City::valueOf);
        LocalDate date = resultSet.getObject("date", LocalDate.class);
        menus.add(mapHeaderRow(resultSet, city, date));
        menuIds.add(resultSet.getLong("id"));
      }
    }
    for (int i = 0; i < menus.size(); i++) {
      menus.get(i).setItemList(loadItems(connection, menuIds.get(i)));
    }
    return menus;
  }

  private Menu mapHeaderRow(ResultSet resultSet, City city, LocalDate date) throws SQLException {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setDate(date.toString());
    menu.setStatus(JdbcMappingSupport.mapEnum(resultSet.getString("status"), Status::valueOf));
    menu.setDeadline(resultSet.getObject("deadline", LocalDateTime.class));
    boolean available = resultSet.getBoolean("available");
    menu.setAvailable(resultSet.wasNull() ? null : available);
    boolean notificated = resultSet.getBoolean("notificated");
    menu.setNotificated(resultSet.wasNull() ? null : notificated);
    menu.setMessage(resultSet.getString("message"));
    return menu;
  }

  private List<Item> loadItems(Connection connection, long menuId) throws SQLException {
    List<Item> items = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(SELECT_ITEMS_BY_MENU_ID)) {
      statement.setLong(1, menuId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          items.add(
              new Item(
                  resultSet.getInt("display_order"),
                  resultSet.getString("name"),
                  JdbcMappingSupport.mapEnum(resultSet.getString("category"), Category::valueOf)));
        }
      }
    }
    return items;
  }

  private long upsertMenu(Connection connection, Menu menu, LocalDate date) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(UPSERT_MENU)) {
      statement.setString(1, menu.getCity().toString());
      statement.setObject(2, date);
      statement.setString(3, menu.getStatus() != null ? menu.getStatus().name() : null);
      statement.setObject(4, menu.getDeadline());
      setNullableBoolean(statement, 5, menu.getAvailable());
      setNullableBoolean(statement, 6, menu.getNotificated());
      statement.setString(7, menu.getMessage());
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong("id");
      }
    }
  }

  private void replaceItems(Connection connection, long menuId, List<Item> items)
      throws SQLException {
    Map<String, Long> existingIdByName = new HashMap<>();
    try (PreparedStatement statement =
        connection.prepareStatement(SELECT_EXISTING_ITEM_IDS_BY_MENU_ID)) {
      statement.setLong(1, menuId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          existingIdByName.put(resultSet.getString("name"), resultSet.getLong("item_id"));
        }
      }
    }

    Set<String> newNames = new HashSet<>();
    for (Item item : items) {
      newNames.add(item.getName());
    }

    try (PreparedStatement statement = connection.prepareStatement(DELETE_ITEM_BY_ID)) {
      for (Map.Entry<String, Long> existing : existingIdByName.entrySet()) {
        if (!newNames.contains(existing.getKey())) {
          statement.setLong(1, existing.getValue());
          statement.addBatch();
        }
      }
      statement.executeBatch();
    }

    try (PreparedStatement statement = connection.prepareStatement(SHIFT_DISPLAY_ORDER_TO_TEMP)) {
      for (Item item : items) {
        Long existingId = existingIdByName.get(item.getName());
        if (existingId != null) {
          statement.setLong(1, existingId);
          statement.addBatch();
        }
      }
      statement.executeBatch();
    }

    try (PreparedStatement updateStatement = connection.prepareStatement(UPDATE_ITEM);
        PreparedStatement insertStatement = connection.prepareStatement(INSERT_ITEM)) {
      for (Item item : items) {
        Long existingId = existingIdByName.get(item.getName());
        if (existingId != null) {
          updateStatement.setInt(1, item.getId());
          updateStatement.setString(2, item.getCategory().name());
          updateStatement.setLong(3, existingId);
          updateStatement.addBatch();
        } else {
          insertStatement.setLong(1, menuId);
          insertStatement.setInt(2, item.getId());
          insertStatement.setString(3, item.getName());
          insertStatement.setString(4, item.getCategory().name());
          insertStatement.addBatch();
        }
      }
      updateStatement.executeBatch();
      insertStatement.executeBatch();
    }
  }

  private void setNullableBoolean(PreparedStatement statement, int index, Boolean value)
      throws SQLException {
    if (value != null) {
      statement.setBoolean(index, value);
    } else {
      statement.setNull(index, Types.BOOLEAN);
    }
  }
}
