/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.Test;

class OrderRepositoryTest extends AbstractDbPersistenceTest {

  private static final LocalDate TODAY = LocalDate.now();

  private final OrderRepository repository = new OrderRepository(PersistenceConfig.getDataSource());
  private final MenuRepository menuRepository =
      new MenuRepository(PersistenceConfig.getDataSource());

  @Test
  void getById_returnsNull_whenOrderWasNeverSaved() {
    assertNull(repository.getById("960000001_" + TODAY, TODAY));
  }

  @Test
  void existById_returnsFalse_whenOrderWasNeverSaved() {
    assertFalse(repository.existById("960000002_" + TODAY, TODAY));
  }

  @Test
  void save_thenGetById_roundTripsHeaderAndItemsAndCategories() {
    String chatId = "960000003";
    ensureUser(chatId);
    Order order = buildOrder(chatId, City.ALMATA, TODAY);
    order.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    order.getOrderItemList().add(new Item(2, "Салат", Category.SALAD));
    order.getCategoryItemList().add(Category.SECOND);
    order.getCategoryItemList().add(Category.SALAD);

    repository.save(order);
    Order found = repository.getById(chatId + "_" + TODAY, TODAY);

    assertEquals(order, found);
  }

  @Test
  void save_calledTwice_replacesItemsAndCategories_insteadOfAppending() {
    String chatId = "960000004";
    ensureUser(chatId);
    Order order = buildOrder(chatId, City.ALMATA, TODAY);
    order.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    order.getCategoryItemList().add(Category.SECOND);
    repository.save(order);

    Order updated = buildOrder(chatId, City.ALMATA, TODAY);
    updated.getOrderItemList().add(new Item(2, "Салат", Category.SALAD));
    updated.getCategoryItemList().add(Category.SALAD);
    repository.save(updated);

    Order found = repository.getById(chatId + "_" + TODAY, TODAY);
    assertEquals(1, found.getOrderItemList().size());
    assertEquals("Салат", found.getOrderItemList().iterator().next().getName());
    assertEquals(Set.of(Category.SALAD), found.getCategoryItemList());
  }

  @Test
  void save_withItemReferencingExistingMenuItem_populatesItemIdFk() throws SQLException {
    String chatId = "960000005";
    ensureUser(chatId);
    Menu menu = buildMenu(City.ASTANA, TODAY, "Борщ", Category.FIRST);
    menuRepository.save(menu);
    Item menuItem = menu.getItemList().get(0);

    Order order = buildOrder(chatId, City.ASTANA, TODAY);
    order.getOrderItemList().add(menuItem);
    repository.save(order);

    Long itemId = findOrderItemId(chatId, TODAY, "Борщ");
    assertEquals(menuItem.getId().longValue(), itemId);
  }

  @Test
  void save_withItemNotPresentInMenuItems_leavesItemIdNull_andDoesNotFail() throws SQLException {
    String chatId = "960000006";
    ensureUser(chatId);
    Order order = buildOrder(chatId, City.KARAGANDA, TODAY);
    order.getOrderItemList().add(new Item(999999, "Забытое блюдо", Category.BAKERY));

    repository.save(order);

    Long itemId = findOrderItemId(chatId, TODAY, "Забытое блюдо");
    assertNull(itemId);
    Order found = repository.getById(chatId + "_" + TODAY, TODAY);
    assertEquals(1, found.getOrderItemList().size());
  }

  @Test
  void deleteById_removesOrder() {
    String chatId = "960000007";
    ensureUser(chatId);
    Order order = buildOrder(chatId, City.ALMATA, TODAY);
    repository.save(order);

    repository.deleteById(chatId + "_" + TODAY, TODAY);

    assertNull(repository.getById(chatId + "_" + TODAY, TODAY));
  }

  @Test
  void getAllByDate_returnsOrdersForThatDateOnly() {
    String chatId1 = "960000008";
    String chatId2 = "960000009";
    ensureUser(chatId1);
    ensureUser(chatId2);
    LocalDate otherDate = TODAY.plusDays(1);
    Order today = buildOrder(chatId1, City.ALMATA, TODAY);
    Order tomorrow = buildOrder(chatId2, City.ALMATA, otherDate);
    repository.save(today);
    repository.save(tomorrow);

    Collection<Order> orders = repository.getAll(TODAY);

    assertTrue(orders.contains(today));
    assertFalse(orders.contains(tomorrow));
  }

  @Test
  void getAllByDate_loadsOwnItemsAndCategories_forEachOrder() {
    String chatId1 = "960000010";
    String chatId2 = "960000011";
    ensureUser(chatId1);
    ensureUser(chatId2);
    Order first = buildOrder(chatId1, City.ALMATA, TODAY);
    first.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    first.getCategoryItemList().add(Category.SECOND);
    Order second = buildOrder(chatId2, City.ALMATA, TODAY);
    second.getOrderItemList().add(new Item(2, "Салат", Category.SALAD));
    second.getOrderItemList().add(new Item(3, "Борщ", Category.FIRST));
    second.getCategoryItemList().add(Category.SALAD);
    second.getCategoryItemList().add(Category.FIRST);
    repository.save(first);
    repository.save(second);

    Map<String, Order> ordersByChatId =
        repository.getAll(TODAY).stream()
            .collect(Collectors.toMap(Order::getChatId, Function.identity()));

    assertEquals(first, ordersByChatId.get(chatId1));
    assertEquals(second, ordersByChatId.get(chatId2));
  }

  @Test
  void getById_returnsEmptyItemsAndCategories_whenOrderHasNone() {
    String chatId = "960000012";
    ensureUser(chatId);
    repository.save(buildOrder(chatId, City.ASTANA, TODAY));

    Order found = repository.getById(chatId + "_" + TODAY, TODAY);

    assertTrue(found.getOrderItemList().isEmpty());
    assertTrue(found.getCategoryItemList().isEmpty());
  }

  private static void ensureUser(String chatId) {
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Repo Test");
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
    Order order = new Order();
    order.setChatId(chatId);
    order.setUsername("Repo Test");
    order.setCity(city);
    order.setStatus(Status.READY);
    order.setDate(date);
    return order;
  }

  private static Menu buildMenu(City city, LocalDate date, String itemName, Category category) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setDate(date.toString());
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(0, itemName, category)));
    menu.setAvailable(true);
    menu.setNotificated(false);
    return menu;
  }
}
