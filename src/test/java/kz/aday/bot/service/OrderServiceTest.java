/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

  private static final LocalDate FRIDAY = LocalDate.of(2026, 9, 11);
  private static final LocalDate SATURDAY = LocalDate.of(2026, 9, 12);
  private static final LocalDate SUNDAY = LocalDate.of(2026, 9, 13);

  private Repository<Order> repository;
  private OrderService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    service = new OrderService();
    repository = mock(Repository.class);
    Field repositoryField = BaseService.class.getDeclaredField("repository");
    repositoryField.setAccessible(true);
    repositoryField.set(service, repository);
  }

  @Test
  void findAllOnDates_mergesOrdersFromAllDates() {
    when(repository.getAll(FRIDAY)).thenReturn(List.of(order("1", "Аня")));
    when(repository.getAll(SATURDAY)).thenReturn(List.of());
    when(repository.getAll(SUNDAY)).thenReturn(List.of(order("2", "Борис")));

    List<String> usernames =
        service.findAllOnDates(List.of(FRIDAY, SATURDAY, SUNDAY)).stream()
            .map(Order::getUsername)
            .toList();

    assertEquals(List.of("Аня", "Борис"), usernames);
  }

  @Test
  void findAllOnDates_keepsLatestOrderOfSameUser() {
    when(repository.getAll(FRIDAY)).thenReturn(List.of(order("1", "Аня в пятницу")));
    when(repository.getAll(SATURDAY)).thenReturn(List.of());
    when(repository.getAll(SUNDAY)).thenReturn(List.of(order("1", "Аня в воскресенье")));

    List<Order> orders = List.copyOf(service.findAllOnDates(List.of(SUNDAY, FRIDAY, SATURDAY)));

    assertEquals(1, orders.size());
    assertEquals("Аня в воскресенье", orders.get(0).getUsername());
  }

  @Test
  void findByIdOnDates_returnsLatestOrderOfUser() {
    when(repository.getAll(FRIDAY)).thenReturn(List.of(order("1", "Аня в пятницу")));
    when(repository.getAll(SATURDAY)).thenReturn(List.of(order("1", "Аня в субботу")));
    when(repository.getAll(SUNDAY)).thenReturn(List.of());

    Optional<Order> order = service.findByIdOnDates("1", List.of(FRIDAY, SATURDAY, SUNDAY));

    assertTrue(order.isPresent());
    assertEquals("Аня в субботу", order.get().getUsername());
  }

  @Test
  void findAllOnDates_keepsSubmittedOrderOverLaterDraft() {
    when(repository.getAll(FRIDAY)).thenReturn(List.of(order("1", "Аня в пятницу")));
    when(repository.getAll(SATURDAY)).thenReturn(List.of(draft("1", "Аня бросила черновик")));
    when(repository.getAll(SUNDAY)).thenReturn(List.of());

    List<Order> orders = List.copyOf(service.findAllOnDates(List.of(FRIDAY, SATURDAY, SUNDAY)));

    assertEquals(1, orders.size());
    assertEquals("Аня в пятницу", orders.get(0).getUsername());
    assertEquals(Status.READY, orders.get(0).getStatus());
  }

  @Test
  void findByIdOnDates_keepsSubmittedOrderOverLaterDraft() {
    when(repository.getAll(FRIDAY)).thenReturn(List.of(order("1", "Аня в пятницу")));
    when(repository.getAll(SATURDAY)).thenReturn(List.of(draft("1", "Аня бросила черновик")));
    when(repository.getAll(SUNDAY)).thenReturn(List.of());

    Optional<Order> order = service.findByIdOnDates("1", List.of(FRIDAY, SATURDAY, SUNDAY));

    assertTrue(order.isPresent());
    assertEquals("Аня в пятницу", order.get().getUsername());
  }

  @Test
  void findAllOnDates_ignoresDraftsWhenNothingSubmitted() {
    when(repository.getAll(FRIDAY)).thenReturn(List.of(draft("1", "черновик в пятницу")));
    when(repository.getAll(SATURDAY)).thenReturn(List.of(draft("1", "черновик в субботу")));
    when(repository.getAll(SUNDAY)).thenReturn(List.of());

    List<Order> orders = List.copyOf(service.findAllOnDates(List.of(FRIDAY, SATURDAY, SUNDAY)));

    assertTrue(orders.isEmpty());
  }

  @Test
  void findByIdOnDates_ignoresNonEmptyDraft() {
    Order draft = draft("1", "незавершённый заказ");
    draft.setOrderItemList(Set.of(new Item(2, "Суп", Category.FIRST)));
    when(repository.getAll(FRIDAY)).thenReturn(List.of(draft));

    assertTrue(service.findByIdOnDates("1", List.of(FRIDAY)).isEmpty());
  }

  @Test
  void findByIdOnDates_returnsEmptyWhenNothingFound() {
    when(repository.getAll(FRIDAY)).thenReturn(List.of(order("2", "Борис")));

    assertTrue(service.findByIdOnDates("1", List.of(FRIDAY)).isEmpty());
    assertTrue(service.findByIdOnDates("1", List.of()).isEmpty());
  }

  /** Подтверждённый непустой заказ — человека кормят. */
  private Order order(String chatId, String username) {
    Order order = new Order();
    order.setChatId(chatId);
    order.setUsername(username);
    order.setCity(City.KARAGANDA);
    order.setStatus(Status.READY);
    order.setOrderItemList(Set.of(new Item(1, "Борщ", Category.FIRST)));
    return order;
  }

  /** Черновик, который бот сохраняет сразу по нажатию "Сделать заказ". */
  private Order draft(String chatId, String username) {
    Order order = order(chatId, username);
    order.setStatus(Status.PENDING);
    order.setOrderItemList(Set.of());
    return order;
  }
}
