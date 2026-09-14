/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import kz.aday.bot.model.*;
import kz.aday.bot.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderService extends BaseService<Order> {
  public OrderService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), Order.class, "order"));
  }

  public Optional<Order> findByIdOnDate(String userId, LocalDate date) {
    log.debug("Finding order by id {} on date {}", userId, date);
    return repository.getAll(date).stream().filter(o -> o.getId().equals(userId)).findFirst();
  }

  public Collection<Order> findAllOnDate(LocalDate date) {
    log.debug("Finding all orders on date {}", date);
    return repository.getAll(date);
  }

  /**
   * Заказы за несколько дней сразу: нужно, чтобы обед понедельника собирался из заказов, сделанных
   * в пятницу, субботу и воскресенье. Если один и тот же пользователь заказывал в несколько дней,
   * остаётся самый поздний подтверждённый заказ, см. {@link #preferred(Order, Order)}.
   */
  public Collection<Order> findAllOnDates(Collection<LocalDate> dates) {
    log.debug("Finding all orders on dates {}", dates);
    Map<String, Order> ordersByUser = new LinkedHashMap<>();
    dates.stream()
        .sorted()
        .forEach(
            date ->
                repository
                    .getAll(date)
                    .forEach(o -> ordersByUser.merge(o.getId(), o, OrderService::preferred)));
    return ordersByUser.values();
  }

  /** Заказ пользователя за несколько дней сразу, самый поздний подтверждённый. */
  public Optional<Order> findByIdOnDates(String userId, Collection<LocalDate> dates) {
    log.debug("Finding order by id {} on dates {}", userId, dates);
    return dates.stream()
        .sorted()
        .map(date -> findByIdOnDate(userId, date))
        .flatMap(Optional::stream)
        .reduce(OrderService::preferred);
  }

  /**
   * Какой из двух заказов одного пользователя считать актуальным. Заказы приходят по возрастанию
   * даты, поэтому обычно побеждает более поздний. Но незавершённый заказ не должен вытеснять
   * подтверждённый: бот сохраняет пустой PENDING сразу по нажатию "Сделать заказ", и такой
   * черновик, брошенный в выходные, иначе стёр бы подтверждённый пятничный заказ из отчёта на
   * понедельник.
   */
  private static Order preferred(Order earlier, Order later) {
    if (isSubmitted(later)) {
      return later;
    }
    return isSubmitted(earlier) ? earlier : later;
  }

  /** Заказ подтверждён и непустой — то есть человека действительно кормят. */
  private static boolean isSubmitted(Order order) {
    return order.getStatus() == Status.READY && !order.getOrderItemList().isEmpty();
  }

  public String getAllOrdersGropedByDate(City city) {
    StringBuilder result = new StringBuilder();
    LocalDate from = LocalDate.now().minusDays(30);
    while (from.isBefore(LocalDate.now())) {
      List<Order> orderList =
          repository.getAll(from).stream().filter(o -> o.getCity() == city).toList();
      result.append(printAttendanceSheetByOrders(orderList, from));
      from = from.plusDays(1);
    }
    return result.toString();
  }

  private String printAttendanceSheetByOrders(Collection<Order> orders, LocalDate date) {
    if (orders.isEmpty()) {
      return String.format("*%s* никто не пришёл.\n", date.toString());
    }
    long peopleCount =
        orders.stream()
            .filter(order -> order.getStatus() == Status.READY)
            .filter(o -> !o.getOrderItemList().isEmpty())
            .count();
    String peopleList =
        orders.stream()
            .filter(order -> order.getStatus() == Status.READY)
            .filter(o -> !o.getOrderItemList().isEmpty())
            .map(Order::getUsername)
            .collect(Collectors.joining(","));
    return String.format(
        "*%s* в офисе заказли еду:*%s*\n%s\n", date.toString(), peopleCount, peopleList);
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
