package kz.aday.bot.service;

import kz.aday.bot.model.Order;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderService {
    private final Repository<Order> repository;

    public OrderService() {
        this.repository = new BaseRepository<>(new HashMap<>(), Order.class, "order");
    }
//
//    public Collection<Order> getPendingOrders() {
//        return repository.getAll().stream()
//                .filter(order -> order.getStatus() == Status.PENDING)
//                .collect(Collectors.toList());
//    }
//
//    public Order getOrder(Long chatId) throws TelegramMessageException {
//        return repository.getById(chatId.toString());
//    }
//
//    public void submitOrder(Long chatId) throws TelegramMessageException {
//        Order order = getOrder(chatId);
//        order.setStatus(Status.READY);
//        repository.save(order);
//    }
//
//    public Order createOrder(Long chatId, String username) {
//        Order order = new Order();
//        order.setChatId(chatId);
//        order.setStatus(Status.PENDING);
//        order.setUsername(username);
//        repository.save(order);
//        return order;
//    }
//
//    public void addOrRemoveItemToOrder(Order order, Item item, MenuRules menuRules) throws TelegramMessageException {
//        if (order.getOrderItemList().contains(item)) {
//            removeItemInOrder(order, item);
//        }
//        if (order.getCategoryItemList().contains(item.getCategory())) {
//            throw new TelegramMessageException("");
//        }
//
//        Set<Category> disjointCategories = menuRules.getMenuRuleMap().get(item.getCategory());
//        if (disjointCategories != null && order.getCategoryItemList().containsAll(disjointCategories)) {
//            throw new TelegramMessageException("Ты уже выбрал, две категории, исключи одну из них чтобы выбрать "+item.getCategory());
//        }
//        order.addItem(item);
//        repository.save(order);
//    }
//
//    private void removeItemInOrder(Order order, Item item) throws TelegramMessageException {
//        order.removeItem(item);
//        repository.save(order);
//    }
//
//    public Report getOrderReport(City city, Set<Long> chatIds) {
//        Collection<Order> allReadyOrders = repository.getAll().stream()
//                .filter(order -> chatIds.contains(order.getChatId()))
//                .filter(order -> order.getStatus() == Status.READY)
//                .collect(Collectors.toList());
//        return new Report(city, allReadyOrders);
//    }
//
//    public void saveOrder(Order order) {
//        repository.save(order);
//    }
}
