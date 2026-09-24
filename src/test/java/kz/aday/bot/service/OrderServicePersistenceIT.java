/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.Test;

class OrderServicePersistenceIT extends AbstractDbPersistenceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 1, 15);

  @Test
  void save_thenFindByChatId_returnsPersistedOrder() {
    String chatId = "970000001";
    ensureUser(chatId);
    Order order = buildOrder(chatId, DATE);
    OrderService service = new OrderService();

    service.save(order);
    Order found = service.findByChatId(chatId, DATE);

    assertEquals(order, found);
  }

  @Test
  void newServiceInstance_seesOrderSavedByPreviousInstance() {
    String chatId = "970000002";
    ensureUser(chatId);
    Order order = buildOrder(chatId, DATE);
    new OrderService().save(order);

    Order found = new OrderService().findByChatId(chatId, DATE);

    assertEquals(order, found);
  }

  @Test
  void submittedAt_survivesSaveAndReload_withSecondPrecision() {
    String chatId = "970000003";
    ensureUser(chatId);
    Order order = buildOrder(chatId, DATE);
    LocalDateTime submittedAt = LocalDateTime.of(2026, 1, 15, 12, 30, 45);
    order.setSubmittedAt(submittedAt);
    new OrderService().save(order);

    Order found = new OrderService().findByChatId(chatId, DATE);

    assertEquals(submittedAt, found.getSubmittedAt());
  }

  @Test
  void deleteByChatId_removesOrderFromStorage() {
    String chatId = "970000004";
    ensureUser(chatId);
    Order order = buildOrder(chatId, DATE);
    OrderService service = new OrderService();
    service.save(order);
    assertTrue(service.existsByChatId(chatId, DATE));

    service.deleteByChatId(chatId, DATE);

    assertFalse(service.existsByChatId(chatId, DATE));
  }

  private static void ensureUser(String chatId) {
    TestUsers.ensureExists(
        PersistenceConfig.getDataSource(), Long.parseLong(chatId), "persistence-test-user");
  }

  private static Order buildOrder(String chatId, LocalDate date) {
    Order order = new Order();
    order.setChatId(chatId);
    order.setUsername("persistence-test-user");
    order.setCity(City.ALMATA);
    order.setStatus(Status.READY);
    order.setDate(date);
    order.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    order.getCategoryItemList().add(Category.SECOND);
    return order;
  }
}
