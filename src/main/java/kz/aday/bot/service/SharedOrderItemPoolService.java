/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItemPool;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.repository.BaseRepository;

public class SharedOrderItemPoolService extends BaseService<SharedOrderItemPool> {
  public SharedOrderItemPoolService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), SharedOrderItemPool.class, "pool"));
  }

  private SharedOrderItemPool getOrCreate(City city) {
    return findByIdOptional(city.toString())
        .orElseGet(
            () -> {
              SharedOrderItemPool sharedOrderItemPool = new SharedOrderItemPool();
              sharedOrderItemPool.setCity(city);
              return sharedOrderItemPool;
            });
  }

  public void addItems(
      City city, String sourceChatId, String sourceUsername, Collection<Item> items) {
    SharedOrderItemPool sharedOrderItemPool = getOrCreate(city);
    for (Item item : items) {
      sharedOrderItemPool.getItems()
          .add(
              new SharedOrderItem(
                  UUID.randomUUID().toString(), item, sourceChatId, sourceUsername, null, null));
    }
    save(sharedOrderItemPool);
  }

  public List<SharedOrderItem> getAvailableEntries(City city) {
    return getOrCreate(city).getItems().stream()
        .filter(entry -> entry.getClaimedByChatId() == null)
        .toList();
  }

  public Optional<SharedOrderItem> claim(
      City city, String entryId, String claimerChatId, String claimerUsername) {
    SharedOrderItemPool sharedOrderItemPool = getOrCreate(city);
    Optional<SharedOrderItem> entry =
        sharedOrderItemPool.getItems().stream()
            .filter(e -> e.getEntryId().equals(entryId) && e.getClaimedByChatId() == null)
            .findFirst();
    entry.ifPresent(
        e -> {
          e.setClaimedByChatId(claimerChatId);
          e.setClaimedByUsername(claimerUsername);
          save(sharedOrderItemPool);
        });
    return entry;
  }
}
