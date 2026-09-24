/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.SharedOrderItemPool;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.JdbcSharedOrderItemPoolRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SharedOrderItemPoolJsonToPostgresMigrator {

  private static final String POOL_STORAGE_PATH = "pool";

  private SharedOrderItemPoolJsonToPostgresMigrator() {}

  public static void main(String[] args) {
    BaseRepository<SharedOrderItemPool> jsonRepository =
        new BaseRepository<>(
            new ConcurrentHashMap<>(), SharedOrderItemPool.class, POOL_STORAGE_PATH);
    JdbcSharedOrderItemPoolRepository postgresRepository =
        new JdbcSharedOrderItemPoolRepository(PersistenceConfig.getDataSource());

    Collection<SharedOrderItemPool> pools = jsonRepository.getAll();
    log.info(
        "Migrating [{}] shared order item pool(s) from JSON storage to Postgres", pools.size());

    for (SharedOrderItemPool pool : pools) {
      forgetLegacyItemIds(pool);
      postgresRepository.save(pool);
    }

    log.info("Migration complete");
  }

  private static void forgetLegacyItemIds(SharedOrderItemPool pool) {
    for (SharedOrderItem entry : pool.getItems()) {
      entry.getItem().setId(null);
    }
  }
}
