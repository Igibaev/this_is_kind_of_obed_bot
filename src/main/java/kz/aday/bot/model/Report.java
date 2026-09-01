/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.util.*;
import lombok.Data;

@Data
public class Report {
  private City city;
  private Collection<Order> orderList;
  private Map<Category, Map<Item, Integer>> itemsCountByCategory = new EnumMap<>(Category.class);

  public Report(City city, Collection<Order> orderList) {
    this.city = city;
    this.orderList = orderList;
  }

  public String printOrderReport() {
    if (orderList.isEmpty()) {
      return "";
    }
    StringBuilder report = new StringBuilder();
    report.append(String.format("*%s*", city.getValue())).append("\n");
    report.append(String.format("*В офис: %s*", orderList.size())).append("\n");
    report.append("\n");

    orderList.forEach(
        order -> {
          order
              .getOrderItemList()
              .forEach(
                  item -> {
                    itemsCountByCategory
                        .computeIfAbsent(item.getCategory(), k -> new LinkedHashMap<>())
                        .merge(item, 1, Integer::sum);
                  });
          report.append(order).append("\n");
        });

    report.append("\n");

    itemsCountByCategory.forEach(
        (category, itemsCount) -> {
          report.append(String.format("*%s*\n", category.getDisplayName()));
          itemsCount.forEach(
              (item, count) -> report.append(item).append(": ").append(count).append(".\n"));
          report.append("\n");
        });

    return report.toString();
  }
}
