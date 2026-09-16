/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItemPool;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SharedOrderItemSharedOrderItemPoolServiceTest {

  private Repository<SharedOrderItemPool> repository;
  private SharedOrderItemPoolService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    service = new SharedOrderItemPoolService();
    repository = mock(Repository.class);
    Field repositoryField = BaseService.class.getDeclaredField("repository");
    repositoryField.setAccessible(true);
    repositoryField.set(service, repository);
  }

  @Test
  void addItems_appendsOneSharedOrderItemPerItem_whenCalled() {
    // given
    when(repository.getById(City.ALMATA.toString())).thenReturn(null);
    Item first = new Item(1, "Плов", null);
    Item second = new Item(2, "Лагман", null);
    // when
    service.addItems(City.ALMATA, "1", "user1", List.of(first, second));
    // then
    SharedOrderItemPool saved = capturedPool();
    assertEquals(2, saved.getItems().size());
    assertTrue(saved.getItems().stream().anyMatch(e -> e.getItem().equals(first)));
    assertTrue(saved.getItems().stream().anyMatch(e -> e.getItem().equals(second)));
    assertTrue(saved.getItems().stream().allMatch(e -> "1".equals(e.getSourceChatId())));
    assertTrue(saved.getItems().stream().allMatch(e -> "user1".equals(e.getSourceUsername())));
    assertTrue(saved.getItems().stream().allMatch(e -> e.getClaimedByChatId() == null));
  }

  @Test
  void getAvailableEntries_excludesClaimedItems_whenSomeClaimed() {
    // given
    SharedOrderItem available = entry("1", "item1", null, null);
    SharedOrderItem claimed = entry("2", "item2", "5", "user5");
    SharedOrderItemPool sharedOrderItemPool = poolWithEntries(City.ALMATA, available, claimed);
    when(repository.getById(City.ALMATA.toString())).thenReturn(sharedOrderItemPool);
    // when
    List<SharedOrderItem> actual = service.getAvailableEntries(City.ALMATA);
    // then
    assertEquals(List.of(available), actual);
  }

  @Test
  void claim_marksEntryClaimed_whenAvailable() {
    // given
    SharedOrderItem available = entry("1", "item1", null, null);
    SharedOrderItemPool sharedOrderItemPool = poolWithEntries(City.ALMATA, available);
    when(repository.getById(City.ALMATA.toString())).thenReturn(sharedOrderItemPool);
    // when
    Optional<SharedOrderItem> actual = service.claim(City.ALMATA, "1", "5", "user5");
    // then
    assertTrue(actual.isPresent());
    assertEquals("5", actual.get().getClaimedByChatId());
    assertEquals("user5", actual.get().getClaimedByUsername());
    verify(repository).save(sharedOrderItemPool);
  }

  @Test
  void claim_returnsEmpty_whenAlreadyClaimedByAnother() {
    // given
    SharedOrderItem claimed = entry("1", "item1", "3", "user3");
    SharedOrderItemPool sharedOrderItemPool = poolWithEntries(City.ALMATA, claimed);
    when(repository.getById(City.ALMATA.toString())).thenReturn(sharedOrderItemPool);
    // when
    Optional<SharedOrderItem> actual = service.claim(City.ALMATA, "1", "5", "user5");
    // then
    assertTrue(actual.isEmpty());
    assertEquals("3", claimed.getClaimedByChatId());
  }

  @Test
  void claim_returnsEmpty_whenEntryIdNotFound() {
    // given
    SharedOrderItemPool sharedOrderItemPool = poolWithEntries(City.ALMATA);
    when(repository.getById(City.ALMATA.toString())).thenReturn(sharedOrderItemPool);
    // when
    Optional<SharedOrderItem> actual = service.claim(City.ALMATA, "unknown", "5", "user5");
    // then
    assertTrue(actual.isEmpty());
  }

  private SharedOrderItemPool capturedPool() {
    ArgumentCaptor<SharedOrderItemPool> captor = ArgumentCaptor.forClass(SharedOrderItemPool.class);
    verify(repository).save(captor.capture());
    return captor.getValue();
  }

  private static SharedOrderItem entry(
      String entryId, String itemName, String claimedByChatId, String claimedByUsername) {
    return new SharedOrderItem(
        entryId, new Item(1, itemName, null), "1", "user1", claimedByChatId, claimedByUsername);
  }

  private static SharedOrderItemPool poolWithEntries(City city, SharedOrderItem... entries) {
    SharedOrderItemPool sharedOrderItemPool = new SharedOrderItemPool();
    sharedOrderItemPool.setCity(city);
    sharedOrderItemPool.getItems().addAll(List.of(entries));
    return sharedOrderItemPool;
  }
}
