/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.MenuRules;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.OrderRepository;
import kz.aday.bot.repository.Repository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderService {
  static final int ORDER_HISTORY_DAYS = 30;
  static final String NOBODY_ORDERED_TEMPLATE = "*%s* никто не пришёл.\n";
  static final String ORDERED_PEOPLE_TEMPLATE = "*%s* в офисе заказали еду:*%s*\n%s\n";
  private static final String USERNAME_DELIMITER = ",";

  private final Repository<Order> repository;

  public OrderService() {
    this(new OrderRepository(PersistenceConfig.getDataSource()));
  }

  OrderService(Repository<Order> repository) {
    this.repository = repository;
  }

  public Order findByChatId(String chatId, LocalDate date) {
    return repository.getById(Order.buildId(chatId, date), date);
  }

  public Optional<Order> findByChatIdOptional(String chatId, LocalDate date) {
    return Optional.ofNullable(findByChatId(chatId, date));
  }

  public boolean existsByChatId(String chatId, LocalDate date) {
    return repository.existById(Order.buildId(chatId, date), date);
  }

  public void deleteByChatId(String chatId, LocalDate date) {
    repository.deleteById(Order.buildId(chatId, date), date);
  }

  public Collection<Order> findAllOnDate(LocalDate date) {
    return repository.getAll(date);
  }

  public Order save(Order order) {
    repository.save(order);
    log.info("Saved order with ID: {}", order.getId());
    return order;
  }

  public Order saveDraft(Order order) {
    order.setStatus(Status.PENDING);
    return save(order);
  }

  public void markOrdersAsSubmitted(City city, LocalDate date) {
    LocalDateTime now = LocalDateTime.now();
    for (Order order : findAllOnDate(date)) {
      if (order.getCity() == city && order.getDate() != null) {
        order.setSubmittedAt(now);
        save(order);
      }
    }
  }

  public String getAllOrdersGroupedByDate(City city) {
    StringBuilder result = new StringBuilder();
    LocalDate today = LocalDate.now();
    for (LocalDate date = today.minusDays(ORDER_HISTORY_DAYS);
        date.isBefore(today);
        date = date.plusDays(1)) {
      List<Order> orders =
          repository.getAll(date).stream().filter(o -> o.getCity() == city).toList();
      result.append(printAttendanceSheetByOrders(orders, date));
    }
    return result.toString();
  }

  public void addItemToOrder(Order order, Item item, MenuRules menuRules) {
    if (order.getOrderItemList().contains(item)) {
      order.getOrderItemList().remove(item);
      order.getCategoryItemList().remove(item.getCategory());
      return;
    }
    if (order.getCategoryItemList().contains(item.getCategory())) {
      removeItemOfCategory(order, item.getCategory());
    } else {
      Set<Category> disjointCategories =
          menuRules.getMenuRuleMap().getOrDefault(item.getCategory(), Collections.emptySet());
      if (!disjointCategories.isEmpty()
          && order.getCategoryItemList().containsAll(disjointCategories)) {
        removeItemOfCategory(order, disjointCategories.iterator().next());
      }
    }
    order.getOrderItemList().add(item);
    order.getCategoryItemList().add(item.getCategory());
  }

  private void removeItemOfCategory(Order order, Category category) {
    order.getOrderItemList().removeIf(it -> it.getCategory() == category);
    order.getCategoryItemList().remove(category);
  }

  private String printAttendanceSheetByOrders(Collection<Order> orders, LocalDate date) {
    if (orders.isEmpty()) {
      return String.format(NOBODY_ORDERED_TEMPLATE, date);
    }
    List<String> usernames =
        orders.stream()
            .filter(order -> order.getStatus() == Status.READY)
            .filter(order -> !order.getOrderItemList().isEmpty())
            .map(Order::getUsername)
            .toList();
    return String.format(
        ORDERED_PEOPLE_TEMPLATE,
        date,
        usernames.size(),
        String.join(USERNAME_DELIMITER, usernames));
  }
}
