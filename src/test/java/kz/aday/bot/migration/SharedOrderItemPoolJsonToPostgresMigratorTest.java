/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.SharedOrderItemPool;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.JdbcSharedOrderItemPoolRepository;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SharedOrderItemPoolJsonToPostgresMigratorTest extends AbstractDbPersistenceTest {

  private static final String POOL_STORAGE_PATH = "pool";
  private static final LocalDate FIRST_DATE = LocalDate.of(2099, 9, 1);
  private static final LocalDate SECOND_DATE = LocalDate.of(2099, 9, 2);
  private static final String SHARER_CHAT_ID = "959000001";
  private static final String CLAIMER_CHAT_ID = "959000002";
  private static final String SHARER_USERNAME = "sharer";
  private static final String CLAIMER_USERNAME = "claimer";
  private static final int LEGACY_ITEM_ID = 7;

  private final BaseRepository<SharedOrderItemPool> jsonRepository =
      new BaseRepository<>(new ConcurrentHashMap<>(), SharedOrderItemPool.class, POOL_STORAGE_PATH);
  private final JdbcSharedOrderItemPoolRepository postgresRepository =
      new JdbcSharedOrderItemPoolRepository(PersistenceConfig.getDataSource());

  @BeforeAll
  static void createUsers() {
    ensureUser(SHARER_CHAT_ID, SHARER_USERNAME);
    ensureUser(CLAIMER_CHAT_ID, CLAIMER_USERNAME);
  }

  @AfterEach
  void cleanUpMigratedPools() {
    for (LocalDate date : List.of(FIRST_DATE, SECOND_DATE)) {
      for (City city : City.values()) {
        String poolId = pool(city, date).getId();
        jsonRepository.deleteById(poolId, date);
        postgresRepository.deleteById(poolId, date);
      }
    }
  }

  @Test
  void main_migratesEveryPoolFromEveryDateFolder_keepingClaims() {
    SharedOrderItem unclaimed = entry("m-1", "Плов", Category.SECOND, null, null);
    SharedOrderItem claimed =
        entry("m-2", "Салат", Category.SALAD, CLAIMER_CHAT_ID, CLAIMER_USERNAME);
    SharedOrderItem otherDate = entry("m-3", "Борщ", Category.FIRST, null, null);
    jsonRepository.save(pool(City.ALMATA, FIRST_DATE, unclaimed, claimed));
    jsonRepository.save(pool(City.ASTANA, SECOND_DATE, otherDate));

    SharedOrderItemPoolJsonToPostgresMigrator.main(new String[0]);

    SharedOrderItemPool migratedAlmaty =
        postgresRepository.getById(pool(City.ALMATA, FIRST_DATE).getId(), FIRST_DATE);
    SharedOrderItemPool migratedAstana =
        postgresRepository.getById(pool(City.ASTANA, SECOND_DATE).getId(), SECOND_DATE);
    assertEquals(List.of(unclaimed, claimed), migratedAlmaty.getItems());
    assertEquals(CLAIMER_CHAT_ID, migratedAlmaty.getItems().get(1).getClaimedByChatId());
    assertEquals(CLAIMER_USERNAME, migratedAlmaty.getItems().get(1).getClaimedByUsername());
    assertEquals(SHARER_CHAT_ID, migratedAlmaty.getItems().get(0).getSourceChatId());
    assertEquals(List.of(otherDate), migratedAstana.getItems());
  }

  @Test
  void main_dropsLegacyItemIds() {
    jsonRepository.save(
        pool(City.KARAGANDA, FIRST_DATE, entry("m-4", "Плов", Category.SECOND, null, null)));

    SharedOrderItemPoolJsonToPostgresMigrator.main(new String[0]);

    SharedOrderItem migrated =
        postgresRepository
            .getById(pool(City.KARAGANDA, FIRST_DATE).getId(), FIRST_DATE)
            .getItems()
            .get(0);
    assertNull(migrated.getItem().getId());
  }

  @Test
  void main_isIdempotent_whenRunTwice() {
    SharedOrderItem entry = entry("m-5", "Плов", Category.SECOND, null, null);
    jsonRepository.save(pool(City.ALMATA, SECOND_DATE, entry));

    SharedOrderItemPoolJsonToPostgresMigrator.main(new String[0]);
    SharedOrderItemPoolJsonToPostgresMigrator.main(new String[0]);

    SharedOrderItemPool migrated =
        postgresRepository.getById(pool(City.ALMATA, SECOND_DATE).getId(), SECOND_DATE);
    assertEquals(List.of(entry), migrated.getItems());
  }

  private static void ensureUser(String chatId, String preferedName) {
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), preferedName);
  }

  private static SharedOrderItem entry(
      String entryId,
      String itemName,
      Category category,
      String claimedByChatId,
      String claimedByUsername) {
    return new SharedOrderItem(
        entryId,
        new Item(LEGACY_ITEM_ID, itemName, category),
        SHARER_CHAT_ID,
        SHARER_USERNAME,
        claimedByChatId,
        claimedByUsername);
  }

  private static SharedOrderItemPool pool(City city, LocalDate date, SharedOrderItem... entries) {
    SharedOrderItemPool pool = new SharedOrderItemPool();
    pool.setCity(city);
    pool.setDate(date);
    pool.getItems().addAll(List.of(entries));
    return pool;
  }
}
