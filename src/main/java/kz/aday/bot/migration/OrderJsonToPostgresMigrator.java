/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.JdbcOrderRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderJsonToPostgresMigrator {

  private static final String ORDER_STORAGE_PATH = "order";

  private OrderJsonToPostgresMigrator() {}

  public static void main(String[] args) {
    BaseRepository<Order> jsonRepository =
        new BaseRepository<>(new ConcurrentHashMap<>(), Order.class, ORDER_STORAGE_PATH);
    JdbcOrderRepository postgresRepository =
        new JdbcOrderRepository(PersistenceConfig.getDataSource());

    Collection<Order> orders = jsonRepository.getAll();
    log.info("Migrating [{}] order(s) from JSON storage to Postgres", orders.size());

    for (Order order : orders) {
      forgetLegacyItemIds(order);
      postgresRepository.save(order);
    }

    log.info("Migration complete");
  }

  private static void forgetLegacyItemIds(Order order) {
    for (Item item : order.getOrderItemList()) {
      item.setId(null);
    }
  }
}
