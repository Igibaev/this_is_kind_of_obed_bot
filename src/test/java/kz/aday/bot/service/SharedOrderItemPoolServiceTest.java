/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.SharedOrderItemPool;
import kz.aday.bot.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SharedOrderItemPoolServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 9, 17);
  private static final LocalDate OTHER_DATE = LocalDate.of(2026, 9, 18);
  private static final String POOL_ID = City.ALMATA + "_" + DATE;

  private Repository<SharedOrderItemPool> repository;
  private SharedOrderItemPoolService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    repository = mock(Repository.class);
    service = new SharedOrderItemPoolService(repository);
  }

  @Test
  void addItems_appendsOneSharedOrderItemPerItem_whenCalled() {
    // given
    when(repository.getById(POOL_ID, DATE)).thenReturn(null);
    Item first = new Item(1, "Плов", null);
    Item second = new Item(2, "Лагман", null);
    // when
    service.addItems(City.ALMATA, DATE, "1", List.of(first, second));
    // then
    SharedOrderItemPool saved = capturedPool();
    assertEquals(2, saved.getItems().size());
    assertTrue(saved.getItems().stream().anyMatch(e -> e.getItem().equals(first)));
    assertTrue(saved.getItems().stream().anyMatch(e -> e.getItem().equals(second)));
    assertTrue(saved.getItems().stream().allMatch(e -> "1".equals(e.getSourceChatId())));
    assertTrue(saved.getItems().stream().allMatch(e -> e.getClaimedByChatId() == null));
  }

  @Test
  void getAvailableEntries_excludesClaimedItems_whenSomeClaimed() {
    // given
    SharedOrderItem available = entry("1", "item1", null);
    SharedOrderItem claimed = entry("2", "item2", "5");
    SharedOrderItemPool sharedOrderItemPool =
        poolWithEntries(City.ALMATA, DATE, available, claimed);
    when(repository.getById(POOL_ID, DATE)).thenReturn(sharedOrderItemPool);
    // when
    List<SharedOrderItem> actual = service.getAvailableEntries(City.ALMATA, DATE);
    // then
    assertEquals(List.of(available), actual);
  }

  @Test
  void getAvailableEntries_doesNotReturnEntriesFromAnotherDate_whenPoolsDiffer() {
    // given
    SharedOrderItem tomorrowEntry = entry("1", "item1", null);
    SharedOrderItemPool tomorrowPool = poolWithEntries(City.ALMATA, OTHER_DATE, tomorrowEntry);
    when(repository.getById(City.ALMATA + "_" + OTHER_DATE, OTHER_DATE)).thenReturn(tomorrowPool);
    when(repository.getById(POOL_ID, DATE)).thenReturn(null);
    // when
    List<SharedOrderItem> actual = service.getAvailableEntries(City.ALMATA, DATE);
    // then
    assertTrue(actual.isEmpty());
  }

  @Test
  void claim_marksEntryClaimed_whenAvailable() {
    // given
    SharedOrderItem available = entry("1", "item1", null);
    SharedOrderItemPool sharedOrderItemPool = poolWithEntries(City.ALMATA, DATE, available);
    when(repository.getById(POOL_ID, DATE)).thenReturn(sharedOrderItemPool);
    // when
    Optional<SharedOrderItem> actual = service.claim(City.ALMATA, DATE, "1", "5");
    // then
    assertTrue(actual.isPresent());
    assertEquals("5", actual.get().getClaimedByChatId());
    verify(repository).save(sharedOrderItemPool);
  }

  @Test
  void claim_returnsEmpty_whenAlreadyClaimedByAnother() {
    // given
    SharedOrderItem claimed = entry("1", "item1", "3");
    SharedOrderItemPool sharedOrderItemPool = poolWithEntries(City.ALMATA, DATE, claimed);
    when(repository.getById(POOL_ID, DATE)).thenReturn(sharedOrderItemPool);
    // when
    Optional<SharedOrderItem> actual = service.claim(City.ALMATA, DATE, "1", "5");
    // then
    assertTrue(actual.isEmpty());
    assertEquals("3", claimed.getClaimedByChatId());
  }

  @Test
  void claim_returnsEmpty_whenEntryIdNotFound() {
    // given
    SharedOrderItemPool sharedOrderItemPool = poolWithEntries(City.ALMATA, DATE);
    when(repository.getById(POOL_ID, DATE)).thenReturn(sharedOrderItemPool);
    // when
    Optional<SharedOrderItem> actual = service.claim(City.ALMATA, DATE, "unknown", "5");
    // then
    assertTrue(actual.isEmpty());
  }

  @Test
  void claim_returnsEmpty_whenEntryBelongsToAnotherDate() {
    // given
    SharedOrderItem tomorrowEntry = entry("1", "item1", null);
    SharedOrderItemPool tomorrowPool = poolWithEntries(City.ALMATA, OTHER_DATE, tomorrowEntry);
    when(repository.getById(City.ALMATA + "_" + OTHER_DATE, OTHER_DATE)).thenReturn(tomorrowPool);
    when(repository.getById(POOL_ID, DATE)).thenReturn(null);
    // when
    Optional<SharedOrderItem> actual = service.claim(City.ALMATA, DATE, "1", "5");
    // then
    assertTrue(actual.isEmpty());
  }

  private SharedOrderItemPool capturedPool() {
    ArgumentCaptor<SharedOrderItemPool> captor = ArgumentCaptor.forClass(SharedOrderItemPool.class);
    verify(repository).save(captor.capture());
    return captor.getValue();
  }

  private static SharedOrderItem entry(String entryId, String itemName, String claimedByChatId) {
    return new SharedOrderItem(entryId, new Item(1, itemName, null), "1", claimedByChatId);
  }

  private static SharedOrderItemPool poolWithEntries(
      City city, LocalDate date, SharedOrderItem... entries) {
    SharedOrderItemPool sharedOrderItemPool = new SharedOrderItemPool();
    sharedOrderItemPool.setCity(city);
    sharedOrderItemPool.setDate(date);
    sharedOrderItemPool.getItems().addAll(List.of(entries));
    return sharedOrderItemPool;
  }
}
