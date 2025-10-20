/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import kz.aday.bot.model.*;
import kz.aday.bot.repository.BaseRepository;

public class OrderService extends BaseService<Order> {
  public OrderService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), Order.class, "order"));
  }

  public String getAllOrdersGropedByDate(City city) {
    StringBuilder result = new StringBuilder();
    LocalDate from = LocalDate.now().minusDays(30);
    while (from.isBefore(LocalDate.now())) {
      List<Order> orderList = repository.getAll(from).stream().filter(o -> o.getCity() == city).toList();
      result.append(printAttendanceSheetByOrders(orderList, from));
      from = from.plusDays(1);
    }
    return result.toString();
  }

  private String printAttendanceSheetByOrders(Collection<Order> orders, LocalDate date) {
    if (orders.isEmpty()) {
      return String.format("*%s* никто не пришёл.\n", date.toString());
    }
    long peopleCount = orders.stream()
            .filter(order -> order.getStatus() == Status.READY)
            .filter(o -> !o.getOrderItemList().isEmpty())
            .count();
    String peopleList = orders.stream()
            .filter(order -> order.getStatus() == Status.READY)
            .filter(o -> !o.getOrderItemList().isEmpty())
            .map(Order::getUsername)
            .collect(Collectors.joining(","));
    return String.format(
            "*%s* в офисе заказли еду:*%s*\n%s\n",
            date.toString(),
            peopleCount,
            peopleList
    );
  }

  public void addItemToOrder(Order order, Item item, MenuRules menuRules) {
    if (order.getOrderItemList().contains(item)) {
      // если пользователь выбрал то что у него уже в заказе, то мы это удалим из заказа
      order.getCategoryItemList().remove(item.getCategory());
      order.getOrderItemList().remove(item);
      return;
    }
    if (order.getCategoryItemList().contains(item.getCategory())) {
      // если пользователь выбрал что-то другое но из той же категории, то мы удаляем то что было в
      // категории, и добавляем новое выбранное
      Item itemToRemove =
          order.getOrderItemList().stream()
              .filter(it -> it.getCategory().equals(item.getCategory()))
              .findFirst()
              .get();
      order.getOrderItemList().remove(itemToRemove);
      order.getOrderItemList().add(item);
      return;
    }
    Set<Category> disjointCategories =
        menuRules.getMenuRuleMap().getOrDefault(item.getCategory(), Collections.emptySet());
    if (disjointCategories != null
        && !disjointCategories.isEmpty()
        && order.getCategoryItemList().containsAll(disjointCategories)) {
      // если сработало правило, то мы удаляем какое нибудь из категории которые у него взаказе и
      // добавляем новое выбранное
      Item itemToRemove =
          order.getOrderItemList().stream()
              .filter(it -> it.getCategory().equals(disjointCategories.stream().findAny().get()))
              .findFirst()
              .get();
      order.getOrderItemList().remove(itemToRemove);
      order.getCategoryItemList().remove(itemToRemove.getCategory());
      order.getOrderItemList().add(item);
      order.getCategoryItemList().add(item.getCategory());
      return;
    }
    // если ничего не выбрал то просто добавляем выбранное
    order.getOrderItemList().add(item);
    order.getCategoryItemList().add(item.getCategory());
  }
}
