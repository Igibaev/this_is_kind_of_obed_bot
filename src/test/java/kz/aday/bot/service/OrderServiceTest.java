/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.MenuRules;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 9, 17);
  private static final String CHAT_ID = "1";
  private static final String ORDER_ID = CHAT_ID + "_" + DATE;
  private static final Item SOUP = new Item(1, "Суп", Category.FIRST);
  private static final Item BORSCH = new Item(2, "Борщ", Category.FIRST);
  private static final Item PLOV = new Item(3, "Плов", Category.SECOND);
  private static final Item CAESAR = new Item(4, "Цезарь", Category.SALAD);
  private static final MenuRules NO_RULES = new MenuRules(City.ALMATA, Map.of());

  private Repository<Order> repository;
  private OrderService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    repository = mock(Repository.class);
    service = new OrderService(repository);
  }

  @Test
  void markOrdersAsSubmitted_stampsSubmittedAt_whenOrderMatchesCityAndHasDate() {
    Order order = orderWithoutSubmittedAt();
    order.setDate(DATE);
    when(repository.getAll(DATE)).thenReturn(List.of(order));

    service.markOrdersAsSubmitted(City.ALMATA, DATE);

    verify(repository).save(order);
    assertNotNull(order.getSubmittedAt());
  }

  @Test
  void markOrdersAsSubmitted_skipsLegacyOrderWithoutDate_toAvoidNullPointerOnSave() {
    Order legacyOrder = orderWithoutSubmittedAt();
    legacyOrder.setDate(null);
    when(repository.getAll(DATE)).thenReturn(List.of(legacyOrder));

    service.markOrdersAsSubmitted(City.ALMATA, DATE);

    verify(repository, never()).save(legacyOrder);
    assertNull(legacyOrder.getSubmittedAt());
  }

  @Test
  void markOrdersAsSubmitted_skipsOrdersFromOtherCities() {
    Order otherCityOrder = orderWithoutSubmittedAt();
    otherCityOrder.setCity(City.KARAGANDA);
    otherCityOrder.setDate(DATE);
    when(repository.getAll(DATE)).thenReturn(List.of(otherCityOrder));

    service.markOrdersAsSubmitted(City.ALMATA, DATE);

    verify(repository, never()).save(otherCityOrder);
  }

  @Test
  void findByChatId_queriesRepositoryByCompositeIdAndDate() {
    Order order = orderWithoutSubmittedAt();
    when(repository.getById(ORDER_ID, DATE)).thenReturn(order);

    assertSame(order, service.findByChatId(CHAT_ID, DATE));
  }

  @Test
  void findByChatIdOptional_returnsOrder_whenPresent() {
    Order order = orderWithoutSubmittedAt();
    when(repository.getById(ORDER_ID, DATE)).thenReturn(order);

    assertEquals(Optional.of(order), service.findByChatIdOptional(CHAT_ID, DATE));
  }

  @Test
  void findByChatIdOptional_returnsEmpty_whenAbsent() {
    when(repository.getById(ORDER_ID, DATE)).thenReturn(null);

    assertTrue(service.findByChatIdOptional(CHAT_ID, DATE).isEmpty());
  }

  @Test
  void existsByChatId_queriesRepositoryByCompositeIdAndDate() {
    when(repository.existById(ORDER_ID, DATE)).thenReturn(true);

    assertTrue(service.existsByChatId(CHAT_ID, DATE));
  }

  @Test
  void deleteByChatId_deletesByCompositeIdAndDate() {
    service.deleteByChatId(CHAT_ID, DATE);

    verify(repository).deleteById(ORDER_ID, DATE);
  }

  @Test
  void findAllOnDate_returnsRepositoryOrdersForDate() {
    Order order = orderWithoutSubmittedAt();
    when(repository.getAll(DATE)).thenReturn(List.of(order));

    assertEquals(List.of(order), List.copyOf(service.findAllOnDate(DATE)));
  }

  @Test
  void saveDraft_setsPendingStatusAndPersists() {
    Order order = orderWithoutSubmittedAt();

    service.saveDraft(order);

    assertEquals(Status.PENDING, order.getStatus());
    verify(repository).save(order);
  }

  @Test
  void addItemToOrder_addsItemAndCategory_whenCategoryNotYetChosen() {
    Order order = new Order();

    service.addItemToOrder(order, SOUP, NO_RULES);

    assertEquals(Set.of(SOUP), order.getOrderItemList());
    assertEquals(Set.of(Category.FIRST), order.getCategoryItemList());
  }

  @Test
  void addItemToOrder_removesItemAndCategory_whenSameItemChosenAgain() {
    Order order = orderWith(SOUP);

    service.addItemToOrder(order, SOUP, NO_RULES);

    assertTrue(order.getOrderItemList().isEmpty());
    assertTrue(order.getCategoryItemList().isEmpty());
  }

  @Test
  void addItemToOrder_replacesItemOfSameCategory_whenAnotherItemOfCategoryChosen() {
    Order order = orderWith(SOUP, PLOV);

    service.addItemToOrder(order, BORSCH, NO_RULES);

    assertEquals(Set.of(BORSCH, PLOV), order.getOrderItemList());
    assertEquals(Set.of(Category.FIRST, Category.SECOND), order.getCategoryItemList());
  }

  @Test
  void addItemToOrder_replacesDisjointItem_whenAllDisjointCategoriesAlreadyChosen() {
    Map<Category, Set<Category>> rules = new EnumMap<>(Category.class);
    rules.put(Category.SALAD, Set.of(Category.FIRST));
    Order order = orderWith(SOUP, PLOV);

    service.addItemToOrder(order, CAESAR, new MenuRules(City.ALMATA, rules));

    assertEquals(Set.of(CAESAR, PLOV), order.getOrderItemList());
    assertEquals(Set.of(Category.SALAD, Category.SECOND), order.getCategoryItemList());
  }

  @Test
  void addItemToOrder_addsItem_whenDisjointCategoryNotChosen() {
    Map<Category, Set<Category>> rules = new EnumMap<>(Category.class);
    rules.put(Category.SALAD, Set.of(Category.FIRST));
    Order order = orderWith(PLOV);

    service.addItemToOrder(order, CAESAR, new MenuRules(City.ALMATA, rules));

    assertEquals(Set.of(CAESAR, PLOV), order.getOrderItemList());
    assertEquals(Set.of(Category.SALAD, Category.SECOND), order.getCategoryItemList());
  }

  @Test
  void getAllOrdersGroupedByDate_coversEachOfLastDays_endingYesterday() {
    when(repository.getAll(any(LocalDate.class))).thenReturn(List.of());
    LocalDate today = LocalDate.now();

    String report = service.getAllOrdersGroupedByDate(City.ALMATA);

    String[] lines = report.split("\n");
    assertEquals(OrderService.ORDER_HISTORY_DAYS, lines.length);
    assertEquals(
        String.format(
                OrderService.NOBODY_ORDERED_TEMPLATE,
                today.minusDays(OrderService.ORDER_HISTORY_DAYS))
            .trim(),
        lines[0]);
    assertEquals(
        String.format(OrderService.NOBODY_ORDERED_TEMPLATE, today.minusDays(1)).trim(),
        lines[lines.length - 1]);
    assertFalse(report.contains(today.toString()));
  }

  @Test
  void getAllOrdersGroupedByDate_listsOnlyReadyNonEmptyOrdersOfCity() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    Order ready = readyOrder("Alice", City.ALMATA, PLOV);
    Order pending = readyOrder("Bob", City.ALMATA, SOUP);
    pending.setStatus(Status.PENDING);
    Order empty = readyOrder("Carol", City.ALMATA);
    Order otherCity = readyOrder("Dave", City.ASTANA, PLOV);
    when(repository.getAll(any(LocalDate.class))).thenReturn(List.of());
    when(repository.getAll(yesterday)).thenReturn(List.of(ready, pending, empty, otherCity));

    String report = service.getAllOrdersGroupedByDate(City.ALMATA);

    assertTrue(
        report.endsWith(
            String.format(OrderService.ORDERED_PEOPLE_TEMPLATE, yesterday, 1, "Alice")));
  }

  private static Order orderWithoutSubmittedAt() {
    Order order = new Order();
    order.setChatId(CHAT_ID);
    order.setCity(City.ALMATA);
    order.setStatus(Status.READY);
    return order;
  }

  private static Order orderWith(Item... items) {
    Order order = new Order();
    for (Item item : items) {
      order.getOrderItemList().add(item);
      order.getCategoryItemList().add(item.getCategory());
    }
    return order;
  }

  private static Order readyOrder(String username, City city, Item... items) {
    Order order = orderWith(items);
    order.setUsername(username);
    order.setCity(city);
    order.setStatus(Status.READY);
    return order;
  }
}
