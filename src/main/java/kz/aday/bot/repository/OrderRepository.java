/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.sql.DataSource;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Id;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.schema.Columns;
import org.springframework.dao.support.DataAccessUtils;

public class OrderRepository extends AbstractRepository<Order> {

  private static final String SELECT_ALL_ORDERS =
      "SELECT o.id, o.chat_id, u.prefered_name AS username, o.city, o.date, o.status, "
          + "o.submitted_at FROM orders o JOIN users u ON u.chat_id = o.chat_id";

  private static final String SELECT_ORDER_BY_CHAT_ID_AND_DATE =
      SELECT_ALL_ORDERS + " WHERE o.chat_id = ? AND o.date = ?";

  private static final String SELECT_ORDERS_BY_DATE = SELECT_ALL_ORDERS + " WHERE o.date = ?";

  private static final String UPSERT_ORDER =
      "INSERT INTO orders (chat_id, city, status, date, submitted_at) "
          + "VALUES (?, ?, ?, ?, ?) "
          + "ON CONFLICT (chat_id, date) DO UPDATE SET "
          + "city = EXCLUDED.city, "
          + "status = EXCLUDED.status, "
          + "submitted_at = EXCLUDED.submitted_at "
          + "RETURNING id";

  private static final String SELECT_ITEMS_BY_ORDER_IDS =
      "SELECT order_id AS owner_id, item_id, name, category FROM order_items "
          + "WHERE order_id IN (:"
          + OWNER_IDS_PARAMETER
          + ")";

  private static final String DELETE_ITEMS_BY_ORDER_ID =
      "DELETE FROM order_items WHERE order_id = ?";

  private static final String INSERT_ITEM =
      "INSERT INTO order_items (order_id, item_id, name, category) "
          + "VALUES (?, (SELECT item_id FROM menu_items WHERE item_id = ?), ?, ?)";

  private static final String SELECT_CATEGORIES_BY_ORDER_IDS =
      "SELECT order_id AS owner_id, category FROM order_categories "
          + "WHERE order_id IN (:"
          + OWNER_IDS_PARAMETER
          + ")";

  private static final String DELETE_CATEGORIES_BY_ORDER_ID =
      "DELETE FROM order_categories WHERE order_id = ?";

  private static final String INSERT_CATEGORY =
      "INSERT INTO order_categories (order_id, category) VALUES (?, ?)";

  private static final String DELETE_ORDER_BY_CHAT_ID_AND_DATE =
      "DELETE FROM orders WHERE chat_id = ? AND date = ?";

  public OrderRepository(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  public Order getById(String id, LocalDate date) {
    return DataAccessUtils.singleResult(
        loadOrders(SELECT_ORDER_BY_CHAT_ID_AND_DATE, toChatId(Id.ownerOf(id)), date));
  }

  @Override
  public Collection<Order> getAll(LocalDate date) {
    return loadOrders(SELECT_ORDERS_BY_DATE, date);
  }

  @Override
  public Collection<Order> getAll() {
    return loadOrders(SELECT_ALL_ORDERS);
  }

  @Override
  public void save(Order order) {
    transactionTemplate.executeWithoutResult(
        status -> {
          long orderId = upsertOrder(order);
          replaceChildren(
              orderId,
              DELETE_ITEMS_BY_ORDER_ID,
              INSERT_ITEM,
              order.getOrderItemList(),
              item -> new Object[] {item.getId(), item.getName(), enumName(item.getCategory())});
          replaceChildren(
              orderId,
              DELETE_CATEGORIES_BY_ORDER_ID,
              INSERT_CATEGORY,
              order.getCategoryItemList(),
              category -> new Object[] {enumName(category)});
        });
    log.info("Saved order [{}]", order.getId());
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    int deleted =
        jdbcTemplate.update(DELETE_ORDER_BY_CHAT_ID_AND_DATE, toChatId(Id.ownerOf(id)), date);
    log.info("Deleted [{}] order(s) with id [{}] for date [{}]", deleted, id, date);
  }

  private List<Order> loadOrders(String sql, Object... args) {
    Map<Long, Order> ordersById = queryIndexedById(sql, OrderRepository::mapHeaderRow, args);
    Map<Long, List<Item>> itemsByOrderId =
        queryGroupedByOwnerId(SELECT_ITEMS_BY_ORDER_IDS, ordersById.keySet(), ITEM_MAPPER);
    Map<Long, List<Category>> categoriesByOrderId =
        queryGroupedByOwnerId(
            SELECT_CATEGORIES_BY_ORDER_IDS,
            ordersById.keySet(),
            (resultSet, rowNum) -> enumValue(resultSet, Columns.CATEGORY, Category.class));
    ordersById.forEach(
        (orderId, order) -> {
          order.setOrderItemList(new HashSet<>(itemsByOrderId.getOrDefault(orderId, List.of())));
          order.setCategoryItemList(
              new HashSet<>(categoriesByOrderId.getOrDefault(orderId, List.of())));
        });
    return new ArrayList<>(ordersById.values());
  }

  private static Order mapHeaderRow(ResultSet resultSet, int rowNum) throws SQLException {
    Order order = new Order();
    order.setChatId(chatIdValue(resultSet, Columns.CHAT_ID));
    order.setUsername(resultSet.getString(Columns.USERNAME));
    order.setCity(enumValue(resultSet, Columns.CITY, City.class));
    order.setStatus(enumValue(resultSet, Columns.STATUS, Status.class));
    order.setDate(resultSet.getObject(Columns.DATE, LocalDate.class));
    order.setSubmittedAt(resultSet.getObject(Columns.SUBMITTED_AT, LocalDateTime.class));
    return order;
  }

  private long upsertOrder(Order order) {
    return jdbcTemplate.queryForObject(
        UPSERT_ORDER,
        Long.class,
        toChatId(order.getChatId()),
        enumName(order.getCity()),
        enumName(order.getStatus()),
        order.getDate(),
        order.getSubmittedAt());
  }

  private <R> void replaceChildren(
      long orderId,
      String deleteSql,
      String insertSql,
      Collection<R> children,
      Function<R, Object[]> childValues) {
    jdbcTemplate.update(deleteSql, orderId);
    jdbcTemplate.batchUpdate(
        insertSql,
        children.stream().map(child -> withOrderId(orderId, childValues.apply(child))).toList());
  }

  private static Object[] withOrderId(long orderId, Object[] values) {
    Object[] row = new Object[values.length + 1];
    row[0] = orderId;
    System.arraycopy(values, 0, row, 1, values.length);
    return row;
  }
}
