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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JdbcOrderRepository implements Repository<Order> {

  private static final String SELECT_ORDER_HEADER =
      "SELECT o.id, u.prefered_name AS username, o.city, o.status, o.submitted_at "
          + "FROM orders o JOIN users u ON u.chat_id = o.chat_id "
          + "WHERE o.chat_id = ? AND o.date = ?";
  private static final String SELECT_ALL_ORDERS =
      "SELECT o.id, o.chat_id, u.prefered_name AS username, o.city, o.date, o.status, "
          + "o.submitted_at FROM orders o JOIN users u ON u.chat_id = o.chat_id";
  private static final String SELECT_ORDERS_BY_DATE = SELECT_ALL_ORDERS + " WHERE o.date = ?";
  private static final String UPSERT_ORDER =
      "INSERT INTO orders (chat_id, city, status, date, submitted_at) "
          + "VALUES (?, ?, ?, ?, ?) "
          + "ON CONFLICT (chat_id, date) DO UPDATE SET "
          + "city = EXCLUDED.city, "
          + "status = EXCLUDED.status, "
          + "submitted_at = EXCLUDED.submitted_at "
          + "RETURNING id";
  private static final String SELECT_ITEMS_BY_ORDER_ID =
      "SELECT item_id, name, category FROM order_items WHERE order_id = ?";
  private static final String DELETE_ITEMS_BY_ORDER_ID =
      "DELETE FROM order_items WHERE order_id = ?";
  private static final String INSERT_ITEM =
      "INSERT INTO order_items (order_id, item_id, name, category) "
          + "VALUES (?, (SELECT item_id FROM menu_items WHERE item_id = ?), ?, ?)";
  private static final String SELECT_CATEGORIES_BY_ORDER_ID =
      "SELECT category FROM order_categories WHERE order_id = ?";
  private static final String DELETE_CATEGORIES_BY_ORDER_ID =
      "DELETE FROM order_categories WHERE order_id = ?";
  private static final String INSERT_CATEGORY =
      "INSERT INTO order_categories (order_id, category) VALUES (?, ?)";
  private static final String DELETE_ORDER_BY_CHAT_ID_AND_DATE =
      "DELETE FROM orders WHERE chat_id = ? AND date = ?";

  private final DataSource dataSource;

  public JdbcOrderRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Order getById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection()) {
      return getById(connection, extractChatId(id), date);
    } catch (SQLException e) {
      log.error("Failure loading order [{}] for date [{}]", id, date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean existById(String id, LocalDate date) {
    return getById(id, date) != null;
  }

  @Override
  public Collection<Order> getAll(LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ORDERS_BY_DATE)) {
      statement.setObject(1, date);
      return loadOrders(connection, statement);
    } catch (SQLException e) {
      log.error("Failure loading orders for date [{}]", date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<Order> getAll() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_ALL_ORDERS)) {
      return loadOrders(connection, statement);
    } catch (SQLException e) {
      log.error("Failure loading orders", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(Order order) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        long orderId = upsertOrder(connection, order);
        replaceItems(connection, orderId, order.getOrderItemList());
        replaceCategories(connection, orderId, order.getCategoryItemList());
        connection.commit();
        log.info("Saved order [{}]", order.getId());
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      log.error("Failure saving order [{}]", order.getId(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearLastWeek() {
    log.info("Clearing order storage is skipped");
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(DELETE_ORDER_BY_CHAT_ID_AND_DATE)) {
      statement.setLong(1, extractChatId(id));
      statement.setObject(2, date);
      int deleted = statement.executeUpdate();
      log.info("Deleted [{}] order(s) with id [{}] for date [{}]", deleted, id, date);
    } catch (SQLException e) {
      log.error("Failure deleting order [{}] for date [{}]", id, date, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clearStorage() {
    log.info("Clearing order storage is skipped");
  }

  private static long extractChatId(String id) {
    return Long.parseLong(id.substring(0, id.indexOf('_')));
  }

  private Order getById(Connection connection, long chatId, LocalDate date) throws SQLException {
    Order order;
    long orderId;
    try (PreparedStatement statement = connection.prepareStatement(SELECT_ORDER_HEADER)) {
      statement.setLong(1, chatId);
      statement.setObject(2, date);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        order = mapHeaderRow(resultSet, String.valueOf(chatId), date);
        orderId = resultSet.getLong("id");
      }
    }
    order.setOrderItemList(loadItems(connection, orderId));
    order.setCategoryItemList(loadCategories(connection, orderId));
    return order;
  }

  private List<Order> loadOrders(Connection connection, PreparedStatement statement)
      throws SQLException {
    List<Order> orders = new ArrayList<>();
    List<Long> orderIds = new ArrayList<>();
    try (ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        String chatId = String.valueOf(resultSet.getLong("chat_id"));
        LocalDate date = resultSet.getObject("date", LocalDate.class);
        orders.add(mapHeaderRow(resultSet, chatId, date));
        orderIds.add(resultSet.getLong("id"));
      }
    }
    for (int i = 0; i < orders.size(); i++) {
      orders.get(i).setOrderItemList(loadItems(connection, orderIds.get(i)));
      orders.get(i).setCategoryItemList(loadCategories(connection, orderIds.get(i)));
    }
    return orders;
  }

  private Order mapHeaderRow(ResultSet resultSet, String chatId, LocalDate date)
      throws SQLException {
    Order order = new Order();
    order.setChatId(chatId);
    order.setUsername(resultSet.getString("username"));
    order.setCity(JdbcMappingSupport.mapEnum(resultSet.getString("city"), City::valueOf));
    order.setStatus(JdbcMappingSupport.mapEnum(resultSet.getString("status"), Status::valueOf));
    order.setDate(date);
    order.setSubmittedAt(resultSet.getObject("submitted_at", LocalDateTime.class));
    return order;
  }

  private Set<Item> loadItems(Connection connection, long orderId) throws SQLException {
    Set<Item> items = new HashSet<>();
    try (PreparedStatement statement = connection.prepareStatement(SELECT_ITEMS_BY_ORDER_ID)) {
      statement.setLong(1, orderId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          int itemId = resultSet.getInt("item_id");
          items.add(
              new Item(
                  resultSet.wasNull() ? null : itemId,
                  resultSet.getString("name"),
                  JdbcMappingSupport.mapEnum(resultSet.getString("category"), Category::valueOf)));
        }
      }
    }
    return items;
  }

  private Set<Category> loadCategories(Connection connection, long orderId) throws SQLException {
    Set<Category> categories = new HashSet<>();
    try (PreparedStatement statement = connection.prepareStatement(SELECT_CATEGORIES_BY_ORDER_ID)) {
      statement.setLong(1, orderId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          categories.add(
              JdbcMappingSupport.mapEnum(resultSet.getString("category"), Category::valueOf));
        }
      }
    }
    return categories;
  }

  private long upsertOrder(Connection connection, Order order) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(UPSERT_ORDER)) {
      statement.setLong(1, Long.parseLong(order.getChatId()));
      statement.setString(2, order.getCity() != null ? order.getCity().name() : null);
      statement.setString(3, order.getStatus() != null ? order.getStatus().name() : null);
      statement.setObject(4, order.getDate());
      statement.setObject(5, order.getSubmittedAt());
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong("id");
      }
    }
  }

  private void replaceItems(Connection connection, long orderId, Set<Item> items)
      throws SQLException {
    try (PreparedStatement deleteStatement =
        connection.prepareStatement(DELETE_ITEMS_BY_ORDER_ID)) {
      deleteStatement.setLong(1, orderId);
      deleteStatement.executeUpdate();
    }
    try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_ITEM)) {
      for (Item item : items) {
        insertStatement.setLong(1, orderId);
        if (item.getId() != null) {
          insertStatement.setLong(2, item.getId());
        } else {
          insertStatement.setNull(2, Types.BIGINT);
        }
        insertStatement.setString(3, item.getName());
        insertStatement.setString(4, item.getCategory().name());
        insertStatement.addBatch();
      }
      insertStatement.executeBatch();
    }
  }

  private void replaceCategories(Connection connection, long orderId, Set<Category> categories)
      throws SQLException {
    try (PreparedStatement deleteStatement =
        connection.prepareStatement(DELETE_CATEGORIES_BY_ORDER_ID)) {
      deleteStatement.setLong(1, orderId);
      deleteStatement.executeUpdate();
    }
    try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_CATEGORY)) {
      for (Category category : categories) {
        insertStatement.setLong(1, orderId);
        insertStatement.setString(2, category.name());
        insertStatement.addBatch();
      }
      insertStatement.executeBatch();
    }
  }
}
