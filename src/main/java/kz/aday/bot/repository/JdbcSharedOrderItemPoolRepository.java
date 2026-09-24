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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.SharedOrderItemPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JdbcSharedOrderItemPoolRepository implements Repository<SharedOrderItemPool> {

  private static final String ID_SEPARATOR = "_";
  private static final String SELECT_ALL_ENTRIES =
      "SELECT entry_id, city, date, item_id, name, category, source_chat_id, claimed_by_chat_id "
          + "FROM shared_order_items";
  private static final String ORDER_BY_INSERTION = " ORDER BY id";
  private static final String SELECT_ENTRIES_BY_CITY_AND_DATE =
      SELECT_ALL_ENTRIES + " WHERE city = ? AND date = ?" + ORDER_BY_INSERTION;
  private static final String SELECT_ENTRIES_BY_DATE =
      SELECT_ALL_ENTRIES + " WHERE date = ?" + ORDER_BY_INSERTION;
  private static final String SELECT_ENTRIES = SELECT_ALL_ENTRIES + ORDER_BY_INSERTION;
  private static final String UPSERT_ENTRY =
      "INSERT INTO shared_order_items (entry_id, city, date, item_id, name, category, "
          + "source_chat_id, claimed_by_chat_id) "
          + "VALUES (?, ?, ?, (SELECT item_id FROM menu_items WHERE item_id = ?), ?, ?, ?, ?) "
          + "ON CONFLICT (entry_id) DO UPDATE SET "
          + "claimed_by_chat_id = EXCLUDED.claimed_by_chat_id "
          + "WHERE shared_order_items.claimed_by_chat_id IS NULL";
  private static final String DELETE_ENTRIES_BY_CITY_AND_DATE =
      "DELETE FROM shared_order_items WHERE city = ? AND date = ?";

  private final DataSource dataSource;

  public JdbcSharedOrderItemPoolRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public SharedOrderItemPool getById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(SELECT_ENTRIES_BY_CITY_AND_DATE)) {
      statement.setString(1, extractCity(id));
      statement.setObject(2, date);
      return loadPools(statement).stream().findFirst().orElse(null);
    } catch (SQLException e) {
      log.error("Failure loading shared order item pool [{}] for date [{}]", id, date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean existById(String id, LocalDate date) {
    return getById(id, date) != null;
  }

  @Override
  public Collection<SharedOrderItemPool> getAll(LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ENTRIES_BY_DATE)) {
      statement.setObject(1, date);
      return loadPools(statement);
    } catch (SQLException e) {
      log.error("Failure loading shared order item pools for date [{}]", date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<SharedOrderItemPool> getAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ENTRIES)) {
      return loadPools(statement);
    } catch (SQLException e) {
      log.error("Failure loading shared order item pools", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(SharedOrderItemPool pool) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPSERT_ENTRY)) {
      for (SharedOrderItem entry : pool.getItems()) {
        bindEntry(statement, pool, entry);
        statement.addBatch();
      }
      statement.executeBatch();
      log.info("Saved shared order item pool [{}]", pool.getId());
    } catch (SQLException e) {
      log.error("Failure saving shared order item pool [{}]", pool.getId(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearLastWeek() {
    log.info("Clearing shared order item pool storage is skipped");
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(DELETE_ENTRIES_BY_CITY_AND_DATE)) {
      statement.setString(1, extractCity(id));
      statement.setObject(2, date);
      int deleted = statement.executeUpdate();
      log.info("Deleted [{}] shared order item(s) of pool [{}] for date [{}]", deleted, id, date);
    } catch (SQLException e) {
      log.error("Failure deleting shared order item pool [{}] for date [{}]", id, date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearStorage() {
    log.info("Clearing shared order item pool storage is skipped");
  }

  private static String extractCity(String id) {
    return id.substring(0, id.indexOf(ID_SEPARATOR));
  }

  private List<SharedOrderItemPool> loadPools(PreparedStatement statement) throws SQLException {
    Map<String, SharedOrderItemPool> pools = new LinkedHashMap<>();
    try (ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        City city = City.valueOf(resultSet.getString("city"));
        LocalDate date = resultSet.getObject("date", LocalDate.class);
        SharedOrderItemPool pool =
            pools.computeIfAbsent(city + ID_SEPARATOR + date, key -> newPool(city, date));
        pool.getItems().add(mapEntry(resultSet));
      }
    }
    return new ArrayList<>(pools.values());
  }

  private static SharedOrderItemPool newPool(City city, LocalDate date) {
    SharedOrderItemPool pool = new SharedOrderItemPool();
    pool.setCity(city);
    pool.setDate(date);
    return pool;
  }

  private static SharedOrderItem mapEntry(ResultSet resultSet) throws SQLException {
    int itemId = resultSet.getInt("item_id");
    Item item =
        new Item(
            resultSet.wasNull() ? null : itemId,
            resultSet.getString("name"),
            JdbcMappingSupport.mapEnum(resultSet.getString("category"), Category::valueOf));
    return new SharedOrderItem(
        resultSet.getString("entry_id"),
        item,
        readChatId(resultSet, "source_chat_id"),
        readChatId(resultSet, "claimed_by_chat_id"));
  }

  private static String readChatId(ResultSet resultSet, String column) throws SQLException {
    long chatId = resultSet.getLong(column);
    return resultSet.wasNull() ? null : String.valueOf(chatId);
  }

  private static void bindEntry(
      PreparedStatement statement, SharedOrderItemPool pool, SharedOrderItem entry)
      throws SQLException {
    Item item = entry.getItem();
    statement.setString(1, entry.getEntryId());
    statement.setString(2, pool.getCity().name());
    statement.setObject(3, pool.getDate());
    bindNullableLong(statement, 4, item.getId() != null ? Long.valueOf(item.getId()) : null);
    statement.setString(5, item.getName());
    statement.setString(6, item.getCategory() != null ? item.getCategory().name() : null);
    bindNullableLong(statement, 7, parseChatId(entry.getSourceChatId()));
    bindNullableLong(statement, 8, parseChatId(entry.getClaimedByChatId()));
  }

  private static Long parseChatId(String chatId) {
    return chatId != null ? Long.valueOf(chatId) : null;
  }

  private static void bindNullableLong(PreparedStatement statement, int index, Long value)
      throws SQLException {
    if (value != null) {
      statement.setLong(index, value);
    } else {
      statement.setNull(index, Types.BIGINT);
    }
  }
}
