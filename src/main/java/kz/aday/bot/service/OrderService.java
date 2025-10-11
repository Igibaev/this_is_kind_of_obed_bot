/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.MenuRules;
import kz.aday.bot.model.Order;
import kz.aday.bot.repository.BaseRepository;

public class OrderService extends BaseService<Order> {
  public OrderService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), Order.class, "order"));
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
    if (disjointCategories != null && !disjointCategories.isEmpty() && order.getCategoryItemList().containsAll(disjointCategories) ) {
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
