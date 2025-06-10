package kz.aday.bot.service;

import kz.aday.bot.model.Order;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.Repository;
import org.glassfish.grizzly.compression.lzma.impl.Base;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderService extends BaseService<Order> {

    public OrderService() {
        super(new BaseRepository<>(new ConcurrentHashMap<>(), Order.class, "order"));
    }
}
