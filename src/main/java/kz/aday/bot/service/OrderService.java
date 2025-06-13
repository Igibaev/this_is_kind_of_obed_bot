/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.Order;
import kz.aday.bot.repository.BaseRepository;

public class OrderService extends BaseService<Order> {

  public OrderService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), Order.class, "order"));
  }
}
