/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Id;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.SharedOrderItemPool;
import kz.aday.bot.repository.schema.Columns;
import org.springframework.dao.support.DataAccessUtils;

public class SharedOrderItemPoolRepository extends AbstractRepository<SharedOrderItemPool> {

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
  private static final String DELETE_ENTRIES_BEFORE =
      "DELETE FROM shared_order_items WHERE date < ?";

  public SharedOrderItemPoolRepository(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  public SharedOrderItemPool getById(String id, LocalDate date) {
    return DataAccessUtils.singleResult(
        loadPools(SELECT_ENTRIES_BY_CITY_AND_DATE, Id.ownerOf(id), date));
  }

  @Override
  public Collection<SharedOrderItemPool> getAll(LocalDate date) {
    return loadPools(SELECT_ENTRIES_BY_DATE, date);
  }

  @Override
  public Collection<SharedOrderItemPool> getAll() {
    return loadPools(SELECT_ENTRIES);
  }

  @Override
  public void save(SharedOrderItemPool pool) {
    List<Object[]> rows = pool.getItems().stream().map(entry -> entryValues(pool, entry)).toList();
    transactionTemplate.executeWithoutResult(
        status -> jdbcTemplate.batchUpdate(UPSERT_ENTRY, rows));
    log.info("Saved shared order item pool [{}]", pool.getId());
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    int deleted = jdbcTemplate.update(DELETE_ENTRIES_BY_CITY_AND_DATE, Id.ownerOf(id), date);
    log.info("Deleted [{}] shared order item(s) of pool [{}] for date [{}]", deleted, id, date);
  }

  public void deleteBefore(LocalDate cutoff) {
    int deleted = jdbcTemplate.update(DELETE_ENTRIES_BEFORE, cutoff);
    log.info("Deleted [{}] shared order item(s) before [{}]", deleted, cutoff);
  }

  private List<SharedOrderItemPool> loadPools(String sql, Object... args) {
    Map<String, SharedOrderItemPool> pools = new LinkedHashMap<>();
    for (SharedOrderItemPool rowPool :
        jdbcTemplate.query(sql, SharedOrderItemPoolRepository::mapRow, args)) {
      pools
          .computeIfAbsent(rowPool.getId(), poolId -> newPool(rowPool.getCity(), rowPool.getDate()))
          .getItems()
          .addAll(rowPool.getItems());
    }
    return new ArrayList<>(pools.values());
  }

  private static SharedOrderItemPool newPool(City city, LocalDate date) {
    SharedOrderItemPool pool = new SharedOrderItemPool();
    pool.setCity(city);
    pool.setDate(date);
    return pool;
  }

  private static SharedOrderItemPool mapRow(ResultSet resultSet, int rowNum) throws SQLException {
    SharedOrderItemPool pool =
        newPool(
            enumValue(resultSet, Columns.CITY, City.class),
            resultSet.getObject(Columns.DATE, LocalDate.class));
    pool.getItems()
        .add(
            new SharedOrderItem(
                resultSet.getString(Columns.ENTRY_ID),
                ITEM_MAPPER.mapRow(resultSet, rowNum),
                chatIdValue(resultSet, Columns.SOURCE_CHAT_ID),
                chatIdValue(resultSet, Columns.CLAIMED_BY_CHAT_ID)));
    return pool;
  }

  private static Object[] entryValues(SharedOrderItemPool pool, SharedOrderItem entry) {
    Item item = entry.getItem();
    return new Object[] {
      entry.getEntryId(),
      enumName(pool.getCity()),
      pool.getDate(),
      item.getId(),
      item.getName(),
      enumName(item.getCategory()),
      toChatId(entry.getSourceChatId()),
      toChatId(entry.getClaimedByChatId())
    };
  }
}
