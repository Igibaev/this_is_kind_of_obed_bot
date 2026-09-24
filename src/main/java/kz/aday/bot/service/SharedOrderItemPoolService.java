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
import kz.aday.bot.repository.JdbcSharedOrderItemPoolRepository;
import kz.aday.bot.repository.Repository;

public class SharedOrderItemPoolService extends BaseService<SharedOrderItemPool> {
  public SharedOrderItemPoolService() {
    super(new JdbcSharedOrderItemPoolRepository(PersistenceConfig.getDataSource()));
  }

  SharedOrderItemPoolService(Repository<SharedOrderItemPool> repository) {
    super(repository);
  }

  private SharedOrderItemPool getOrCreate(City city, LocalDate date) {
    SharedOrderItemPool existing = repository.getById(city + "_" + date, date);
    if (existing != null) {
      return existing;
    }
    SharedOrderItemPool sharedOrderItemPool = new SharedOrderItemPool();
    sharedOrderItemPool.setCity(city);
    sharedOrderItemPool.setDate(date);
    return sharedOrderItemPool;
  }

  public void addItems(City city, LocalDate date, String sourceChatId, Collection<Item> items) {
    SharedOrderItemPool sharedOrderItemPool = getOrCreate(city, date);
    for (Item item : items) {
      sharedOrderItemPool
          .getItems()
          .add(new SharedOrderItem(UUID.randomUUID().toString(), item, sourceChatId, null));
    }
    save(sharedOrderItemPool);
  }

  public List<SharedOrderItem> getAvailableEntries(City city, LocalDate date) {
    return getOrCreate(city, date).getItems().stream()
        .filter(entry -> entry.getClaimedByChatId() == null)
        .toList();
  }

  public Optional<SharedOrderItem> claim(
      City city, LocalDate date, String entryId, String claimerChatId) {
    SharedOrderItemPool sharedOrderItemPool = getOrCreate(city, date);
    Optional<SharedOrderItem> entry =
        sharedOrderItemPool.getItems().stream()
            .filter(e -> e.getEntryId().equals(entryId) && e.getClaimedByChatId() == null)
            .findFirst();
    entry.ifPresent(
        e -> {
          e.setClaimedByChatId(claimerChatId);
          save(sharedOrderItemPool);
        });
    return entry;
  }
}
