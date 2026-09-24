/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.SharedOrderItemPool;
import kz.aday.bot.repository.SharedOrderItemPoolRepository;

public class SharedOrderItemPoolService {
  private final SharedOrderItemPoolRepository repository;

  public SharedOrderItemPoolService() {
    this(new SharedOrderItemPoolRepository(PersistenceConfig.getDataSource()));
  }

  SharedOrderItemPoolService(SharedOrderItemPoolRepository repository) {
    this.repository = repository;
  }

  public void deleteOutdated() {
    repository.deleteBefore(LocalDate.now());
  }

  public void addItems(City city, LocalDate date, String sourceChatId, Collection<Item> items) {
    SharedOrderItemPool sharedOrderItemPool = getOrCreate(city, date);
    for (Item item : items) {
      sharedOrderItemPool
          .getItems()
          .add(new SharedOrderItem(UUID.randomUUID().toString(), item, sourceChatId, null));
    }
    repository.save(sharedOrderItemPool);
  }

  public List<SharedOrderItem> getAvailableEntries(City city, LocalDate date) {
    return getOrCreate(city, date).getItems().stream()
        .filter(SharedOrderItem::isAvailable)
        .toList();
  }

  public Optional<SharedOrderItem> claim(
      City city, LocalDate date, String entryId, String claimerChatId) {
    SharedOrderItemPool sharedOrderItemPool = getOrCreate(city, date);
    Optional<SharedOrderItem> entry =
        sharedOrderItemPool.getItems().stream()
            .filter(SharedOrderItem::isAvailable)
            .filter(e -> e.getEntryId().equals(entryId))
            .findFirst();
    entry.ifPresent(
        e -> {
          e.setClaimedByChatId(claimerChatId);
          repository.save(sharedOrderItemPool);
        });
    return entry;
  }

  private SharedOrderItemPool getOrCreate(City city, LocalDate date) {
    SharedOrderItemPool existing =
        repository.getById(SharedOrderItemPool.buildId(city, date), date);
    if (existing != null) {
      return existing;
    }
    SharedOrderItemPool sharedOrderItemPool = new SharedOrderItemPool();
    sharedOrderItemPool.setCity(city);
    sharedOrderItemPool.setDate(date);
    return sharedOrderItemPool;
  }
}
