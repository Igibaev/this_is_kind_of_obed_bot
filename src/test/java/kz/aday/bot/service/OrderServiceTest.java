/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 9, 17);

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
    // given
    Order order = orderWithoutSubmittedAt();
    order.setDate(DATE);
    when(repository.getAll(DATE)).thenReturn(List.of(order));
    // when
    service.markOrdersAsSubmitted(City.ALMATA, DATE);
    // then
    verify(repository).save(order);
    org.junit.jupiter.api.Assertions.assertNotNull(order.getSubmittedAt());
  }

  @Test
  void markOrdersAsSubmitted_skipsLegacyOrderWithoutDate_toAvoidNullPointerOnSave() {
    // given: заказ старого формата (до перехода на составной id) - дата не сохранена в JSON
    Order legacyOrder = orderWithoutSubmittedAt();
    legacyOrder.setDate(null);
    when(repository.getAll(DATE)).thenReturn(List.of(legacyOrder));
    // when
    service.markOrdersAsSubmitted(City.ALMATA, DATE);
    // then
    verify(repository, never()).save(legacyOrder);
    org.junit.jupiter.api.Assertions.assertNull(legacyOrder.getSubmittedAt());
  }

  @Test
  void markOrdersAsSubmitted_skipsOrdersFromOtherCities() {
    // given
    Order otherCityOrder = orderWithoutSubmittedAt();
    otherCityOrder.setCity(City.KARAGANDA);
    otherCityOrder.setDate(DATE);
    when(repository.getAll(DATE)).thenReturn(List.of(otherCityOrder));
    // when
    service.markOrdersAsSubmitted(City.ALMATA, DATE);
    // then
    verify(repository, never()).save(otherCityOrder);
  }

  private static Order orderWithoutSubmittedAt() {
    Order order = new Order();
    order.setChatId("1");
    order.setCity(City.ALMATA);
    order.setStatus(Status.READY);
    return order;
  }
}
