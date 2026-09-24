/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.JdbcOrderRepository;
import kz.aday.bot.repository.JsonFileStorageSupport;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OrderJsonToPostgresMigratorTest extends AbstractDbPersistenceTest {

  private static final String ORDER_STORAGE_PATH = "order";
  private static final LocalDate TODAY = LocalDate.now();
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);
  private static final String FIRST_CHAT_ID = "980000010";
  private static final String SECOND_CHAT_ID = "980000011";
  private static final String LEGACY_CHAT_ID = "980000012";

  private final BaseRepository<Order> jsonRepository =
      new BaseRepository<>(new ConcurrentHashMap<>(), Order.class, ORDER_STORAGE_PATH);
  private final JdbcOrderRepository postgresRepository =
      new JdbcOrderRepository(PersistenceConfig.getDataSource());

  @AfterEach
  void cleanUpMigratedOrders() {
    jsonRepository.deleteById(FIRST_CHAT_ID + "_" + TODAY, TODAY);
    jsonRepository.deleteById(SECOND_CHAT_ID + "_" + YESTERDAY, YESTERDAY);
    jsonRepository.deleteById(LEGACY_CHAT_ID + "_" + TODAY, TODAY);
    postgresRepository.deleteById(FIRST_CHAT_ID + "_" + TODAY, TODAY);
    postgresRepository.deleteById(SECOND_CHAT_ID + "_" + YESTERDAY, YESTERDAY);
    postgresRepository.deleteById(LEGACY_CHAT_ID + "_" + TODAY, TODAY);
  }

  @Test
  void main_migratesEveryOrderFromJsonStorageIntoPostgres_acrossDifferentDateFolders() {
    Order first = buildOrder(FIRST_CHAT_ID, City.ALMATA, TODAY);
    Order second = buildOrder(SECOND_CHAT_ID, City.ASTANA, YESTERDAY);
    jsonRepository.save(first);
    jsonRepository.save(second);

    OrderJsonToPostgresMigrator.main(new String[0]);

    Order migratedFirst = postgresRepository.getById(first.getId(), TODAY);
    Order migratedSecond = postgresRepository.getById(second.getId(), YESTERDAY);

    assertEquals(City.ALMATA, migratedFirst.getCity());
    assertEquals(City.ASTANA, migratedSecond.getCity());
    assertEquals(Set.of("Плов"), itemNames(migratedFirst));
  }

  @Test
  void main_overwritesExistingRow_whenOrderWasAlreadyMigratedBefore() {
    Order order = buildOrder(FIRST_CHAT_ID, City.ALMATA, TODAY);
    jsonRepository.save(order);
    Order stale = buildOrder(FIRST_CHAT_ID, City.ASTANA, TODAY);
    stale.setStatus(Status.DEADLINE);
    postgresRepository.save(stale);

    OrderJsonToPostgresMigrator.main(new String[0]);

    Order migrated = postgresRepository.getById(order.getId(), TODAY);
    assertEquals(City.ALMATA, migrated.getCity());
    assertEquals(Status.READY, migrated.getStatus());
  }

  @Test
  void main_doesNothing_whenNoJsonOrdersExist() {
    OrderJsonToPostgresMigrator.main(new String[0]);

    assertNull(postgresRepository.getById(FIRST_CHAT_ID + "_" + TODAY, TODAY));
  }

  @Test
  void main_ignoresLegacyItemId_whenPopulatingOrderItemsFk() throws SQLException {
    Order order = buildOrder(FIRST_CHAT_ID, City.ALMATA, TODAY);
    jsonRepository.save(order);

    OrderJsonToPostgresMigrator.main(new String[0]);

    assertNull(findOrderItemId(FIRST_CHAT_ID, TODAY, "Плов"));
  }

  @Test
  void main_backfillsDateFromFolder_whenJsonOrderIsMissingDateField() throws Exception {
    TestUsers.ensureExists(
        PersistenceConfig.getDataSource(), Long.parseLong(LEGACY_CHAT_ID), "Legacy");
    Path folder =
        Path.of(BotConfig.getBotStorePath()).resolve(ORDER_STORAGE_PATH).resolve(TODAY.toString());
    Files.createDirectories(folder);
    Files.writeString(
        folder.resolve(LEGACY_CHAT_ID + "_" + TODAY + JsonFileStorageSupport.JSON),
        "{\"chatId\":\""
            + LEGACY_CHAT_ID
            + "\",\"city\":\"ALMATA\",\"status\":\"READY\",\"orderItemList\":[],"
            + "\"categoryItemList\":[]}");

    OrderJsonToPostgresMigrator.main(new String[0]);

    assertEquals(TODAY, findStoredDate(LEGACY_CHAT_ID));
  }

  private static LocalDate findStoredDate(String chatId) throws SQLException {
    String sql = "SELECT date FROM orders WHERE chat_id = ?";
    try (Connection connection = PersistenceConfig.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, Long.parseLong(chatId));
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getObject("date", LocalDate.class);
      }
    }
  }

  private static Set<String> itemNames(Order order) {
    return order.getOrderItemList().stream().map(Item::getName).collect(Collectors.toSet());
  }

  private static Long findOrderItemId(String chatId, LocalDate date, String itemName)
      throws SQLException {
    String sql =
        "SELECT oi.item_id FROM order_items oi "
            + "JOIN orders o ON o.id = oi.order_id "
            + "WHERE o.chat_id = ? AND o.date = ? AND oi.name = ?";
    try (Connection connection = PersistenceConfig.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, Long.parseLong(chatId));
      statement.setObject(2, date);
      statement.setString(3, itemName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        long itemId = resultSet.getLong("item_id");
        return resultSet.wasNull() ? null : itemId;
      }
    }
  }

  private static Order buildOrder(String chatId, City city, LocalDate date) {
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Migrated");
    Order order = new Order();
    order.setChatId(chatId);
    order.setCity(city);
    order.setStatus(Status.READY);
    order.setDate(date);
    order.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    order.getCategoryItemList().add(Category.SECOND);
    return order;
  }
}
