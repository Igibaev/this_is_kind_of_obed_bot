/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import kz.aday.bot.exception.TelegramMessageException;
import lombok.Data;

@Data
public class Report {
  private City city;
  private Collection<Order> orderList;
  private Map<Item, Integer> itemsCount = new HashMap<>();

  public Report(City city, Collection<Order> orderList) {
    this.city = city;
    this.orderList = orderList;
  }

  public String printOrderReport() throws TelegramMessageException {
    if (orderList.isEmpty()) {
      throw new TelegramMessageException("");
    }
    StringBuilder report = new StringBuilder();
    report.append(city).append("\n");
    report.append("\n");
    orderList.forEach(
        order -> {
          order
              .getOrderItemList()
              .forEach(
                  item -> {
                    Integer count = itemsCount.getOrDefault(item, 0);
                    itemsCount.put(item, ++count);
                  });
          report.append(order).append("\n");
        });
    report.append("\n");
    itemsCount.forEach(
        (item, count) -> report.append(item).append(": ").append(count).append(".\n"));
    return report.toString();
  }
}
