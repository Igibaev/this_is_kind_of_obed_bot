/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.SharedOrderItemPool;
import kz.aday.bot.model.Status;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

class SharedOrderItemPoolRepositoryTest extends AbstractDbPersistenceTest {

  private static final LocalDate BASE_DATE = LocalDate.of(2099, 7, 1);
  private static final LocalDate DELETION_CUTOFF = LocalDate.of(1985, 6, 1);
  private static final LocalDate BEFORE_DELETION_CUTOFF = DELETION_CUTOFF.minusDays(1);
  private static final String TEST_USER_NAME = "pool-test-user";
  private static final String SOURCE_CHAT_ID = "957000001";
  private static final String FIRST_CLAIMER_CHAT_ID = "957000002";
  private static final String SECOND_CLAIMER_CHAT_ID = "957000003";
  private static final int MISSING_MENU_ITEM_ID = 999999;
  private static final String UNKNOWN_USER_CHAT_ID = "957999999";

  private final SharedOrderItemPoolRepository repository =
      new SharedOrderItemPoolRepository(PersistenceConfig.getDataSource());
  private final MenuRepository menuRepository =
      new MenuRepository(PersistenceConfig.getDataSource());

  @BeforeAll
  static void createUsers() {
    ensureUser(SOURCE_CHAT_ID);
    ensureUser(FIRST_CLAIMER_CHAT_ID);
    ensureUser(SECOND_CLAIMER_CHAT_ID);
  }

  @Test
  void getById_returnsNull_whenPoolWasNeverSaved() {
    LocalDate date = BASE_DATE;

    assertNull(repository.getById(poolId(City.ALMATA, date), date));
  }

  @Test
  void existById_returnsFalse_whenPoolWasNeverSaved() {
    LocalDate date = BASE_DATE.plusDays(1);

    assertFalse(repository.existById(poolId(City.ALMATA, date), date));
  }

  @Test
  void existById_returnsTrue_whenPoolWasSaved() {
    LocalDate date = BASE_DATE.plusDays(2);
    repository.save(pool(City.ALMATA, date, entry("e-2-1", "Плов", Category.SECOND)));

    assertTrue(repository.existById(poolId(City.ALMATA, date), date));
  }

  @Test
  void save_thenGetById_roundTripsEveryEntryField_inInsertionOrder() {
    LocalDate date = BASE_DATE.plusDays(3);
    SharedOrderItem unclaimed = entry("e-3-1", "Плов", Category.SECOND);
    SharedOrderItem claimed = entry("e-3-2", "Салат", Category.SALAD);
    claimed.setClaimedByChatId(FIRST_CLAIMER_CHAT_ID);
    repository.save(pool(City.ALMATA, date, unclaimed, claimed));

    SharedOrderItemPool found = repository.getById(poolId(City.ALMATA, date), date);

    assertEquals(City.ALMATA, found.getCity());
    assertEquals(date, found.getDate());
    assertEquals(List.of(unclaimed, claimed), found.getItems());
    assertEquals(Category.SALAD, found.getItems().get(1).getItem().getCategory());
    assertEquals(SOURCE_CHAT_ID, found.getItems().get(1).getSourceChatId());
    assertEquals(FIRST_CLAIMER_CHAT_ID, found.getItems().get(1).getClaimedByChatId());
  }

  @Test
  void save_thenGetById_keepsNullCategoryAndNullSource() {
    LocalDate date = BASE_DATE.plusDays(4);
    SharedOrderItem anonymous =
        new SharedOrderItem("e-4-1", new Item(null, "Хлеб", null), null, null);
    repository.save(pool(City.ALMATA, date, anonymous));

    SharedOrderItem found = repository.getById(poolId(City.ALMATA, date), date).getItems().get(0);

    assertNull(found.getItem().getCategory());
    assertNull(found.getSourceChatId());
  }

  @Test
  void getById_returnsOnlyEntriesOfRequestedCity() {
    LocalDate date = BASE_DATE.plusDays(5);
    SharedOrderItem almatyEntry = entry("e-5-1", "Плов", Category.SECOND);
    SharedOrderItem astanaEntry = entry("e-5-2", "Борщ", Category.FIRST);
    repository.save(pool(City.ALMATA, date, almatyEntry));
    repository.save(pool(City.ASTANA, date, astanaEntry));

    SharedOrderItemPool found = repository.getById(poolId(City.ALMATA, date), date);

    assertEquals(List.of(almatyEntry), found.getItems());
  }

  @Test
  void save_calledAgainWithAddedEntry_appendsWithoutDuplicatingExistingEntries() {
    LocalDate date = BASE_DATE.plusDays(6);
    SharedOrderItem first = entry("e-6-1", "Плов", Category.SECOND);
    SharedOrderItem second = entry("e-6-2", "Салат", Category.SALAD);
    repository.save(pool(City.ALMATA, date, first));

    repository.save(pool(City.ALMATA, date, first, second));

    SharedOrderItemPool found = repository.getById(poolId(City.ALMATA, date), date);
    assertEquals(List.of(first, second), found.getItems());
  }

  @Test
  void save_withClaimedEntry_persistsClaim() {
    LocalDate date = BASE_DATE.plusDays(7);
    SharedOrderItem entry = entry("e-7-1", "Плов", Category.SECOND);
    repository.save(pool(City.ALMATA, date, entry));
    entry.setClaimedByChatId(FIRST_CLAIMER_CHAT_ID);

    repository.save(pool(City.ALMATA, date, entry));

    SharedOrderItem found = repository.getById(poolId(City.ALMATA, date), date).getItems().get(0);
    assertEquals(FIRST_CLAIMER_CHAT_ID, found.getClaimedByChatId());
  }

  @Test
  void save_withEntryAlreadyClaimedInStorage_keepsFirstClaim() {
    LocalDate date = BASE_DATE.plusDays(8);
    SharedOrderItem firstClaim = entry("e-8-1", "Плов", Category.SECOND);
    firstClaim.setClaimedByChatId(FIRST_CLAIMER_CHAT_ID);
    repository.save(pool(City.ALMATA, date, firstClaim));
    SharedOrderItem secondClaim = entry("e-8-1", "Плов", Category.SECOND);
    secondClaim.setClaimedByChatId(SECOND_CLAIMER_CHAT_ID);

    repository.save(pool(City.ALMATA, date, secondClaim));

    SharedOrderItem found = repository.getById(poolId(City.ALMATA, date), date).getItems().get(0);
    assertEquals(FIRST_CLAIMER_CHAT_ID, found.getClaimedByChatId());
  }

  @Test
  void save_withItemReferencingExistingMenuItem_keepsItemId() {
    LocalDate date = BASE_DATE.plusDays(9);
    Menu menu = buildMenu(City.KARAGANDA, date, "Борщ", Category.FIRST);
    menuRepository.save(menu);
    Item menuItem = menu.getItemList().get(0);
    repository.save(
        pool(City.KARAGANDA, date, new SharedOrderItem("e-9-1", menuItem, SOURCE_CHAT_ID, null)));

    SharedOrderItem found =
        repository.getById(poolId(City.KARAGANDA, date), date).getItems().get(0);

    assertEquals(menuItem.getId(), found.getItem().getId());
  }

  @Test
  void save_withItemNotPresentInMenuItems_leavesItemIdNull_andDoesNotFail() {
    LocalDate date = BASE_DATE.plusDays(10);
    SharedOrderItem entry =
        new SharedOrderItem(
            "e-10-1",
            new Item(MISSING_MENU_ITEM_ID, "Забытое блюдо", Category.BAKERY),
            SOURCE_CHAT_ID,
            null);

    repository.save(pool(City.ALMATA, date, entry));

    SharedOrderItem found = repository.getById(poolId(City.ALMATA, date), date).getItems().get(0);
    assertNull(found.getItem().getId());
    assertEquals("Забытое блюдо", found.getItem().getName());
  }

  @Test
  void getAllByDate_returnsOnePoolPerCity_forThatDateOnly() {
    LocalDate date = BASE_DATE.plusDays(11);
    LocalDate otherDate = BASE_DATE.plusDays(12);
    SharedOrderItem almatyEntry = entry("e-11-1", "Плов", Category.SECOND);
    SharedOrderItem astanaEntry = entry("e-11-2", "Борщ", Category.FIRST);
    SharedOrderItem otherDateEntry = entry("e-12-1", "Салат", Category.SALAD);
    repository.save(pool(City.ALMATA, date, almatyEntry));
    repository.save(pool(City.ASTANA, date, astanaEntry));
    repository.save(pool(City.ALMATA, otherDate, otherDateEntry));

    Collection<SharedOrderItemPool> pools = repository.getAll(date);

    assertEquals(2, pools.size());
    assertTrue(pools.contains(pool(City.ALMATA, date, almatyEntry)));
    assertTrue(pools.contains(pool(City.ASTANA, date, astanaEntry)));
  }

  @Test
  void getAll_returnsPoolsFromEveryDate() {
    LocalDate date = BASE_DATE.plusDays(13);
    LocalDate otherDate = BASE_DATE.plusDays(14);
    SharedOrderItem entry = entry("e-13-1", "Плов", Category.SECOND);
    SharedOrderItem otherDateEntry = entry("e-14-1", "Салат", Category.SALAD);
    repository.save(pool(City.ALMATA, date, entry));
    repository.save(pool(City.ALMATA, otherDate, otherDateEntry));

    Collection<SharedOrderItemPool> pools = repository.getAll();

    assertTrue(pools.contains(pool(City.ALMATA, date, entry)));
    assertTrue(pools.contains(pool(City.ALMATA, otherDate, otherDateEntry)));
  }

  @Test
  void deleteById_removesOnlyPoolOfThatCityAndDate() {
    LocalDate date = BASE_DATE.plusDays(15);
    LocalDate otherDate = BASE_DATE.plusDays(16);
    SharedOrderItem astanaEntry = entry("e-15-2", "Борщ", Category.FIRST);
    SharedOrderItem otherDateEntry = entry("e-16-1", "Салат", Category.SALAD);
    repository.save(pool(City.ALMATA, date, entry("e-15-1", "Плов", Category.SECOND)));
    repository.save(pool(City.ASTANA, date, astanaEntry));
    repository.save(pool(City.ALMATA, otherDate, otherDateEntry));

    repository.deleteById(poolId(City.ALMATA, date), date);

    assertNull(repository.getById(poolId(City.ALMATA, date), date));
    assertEquals(
        List.of(astanaEntry), repository.getById(poolId(City.ASTANA, date), date).getItems());
    assertEquals(
        List.of(otherDateEntry),
        repository.getById(poolId(City.ALMATA, otherDate), otherDate).getItems());
  }

  @Test
  void save_rollsBackWholePool_whenOneEntryFails() {
    LocalDate date = BASE_DATE.plusDays(17);
    SharedOrderItem valid = entry("e-17-1", "Плов", Category.SECOND);
    SharedOrderItem invalid =
        new SharedOrderItem(
            "e-17-2", new Item(null, "Салат", Category.SALAD), UNKNOWN_USER_CHAT_ID, null);

    assertThrows(
        DataAccessException.class, () -> repository.save(pool(City.ALMATA, date, valid, invalid)));

    assertNull(repository.getById(poolId(City.ALMATA, date), date));
  }

  @Test
  void deleteBefore_removesPoolsOfEveryCityDatedBeforeCutoff() {
    LocalDate earlier = BEFORE_DELETION_CUTOFF.minusDays(1);
    repository.save(pool(City.ALMATA, BEFORE_DELETION_CUTOFF, entry("d-1-1", "Плов", null)));
    repository.save(pool(City.ASTANA, earlier, entry("d-1-2", "Борщ", null)));

    repository.deleteBefore(DELETION_CUTOFF);

    assertNull(
        repository.getById(poolId(City.ALMATA, BEFORE_DELETION_CUTOFF), BEFORE_DELETION_CUTOFF));
    assertNull(repository.getById(poolId(City.ASTANA, earlier), earlier));
  }

  @Test
  void deleteBefore_keepsPoolsDatedOnOrAfterCutoff() {
    LocalDate afterCutoff = DELETION_CUTOFF.plusDays(1);
    SharedOrderItem onCutoffEntry = entry("d-2-1", "Плов", Category.SECOND);
    SharedOrderItem afterCutoffEntry = entry("d-2-2", "Салат", Category.SALAD);
    repository.save(pool(City.KARAGANDA, DELETION_CUTOFF, onCutoffEntry));
    repository.save(pool(City.KARAGANDA, afterCutoff, afterCutoffEntry));

    repository.deleteBefore(DELETION_CUTOFF);

    assertEquals(
        List.of(onCutoffEntry),
        repository.getById(poolId(City.KARAGANDA, DELETION_CUTOFF), DELETION_CUTOFF).getItems());
    assertEquals(
        List.of(afterCutoffEntry),
        repository.getById(poolId(City.KARAGANDA, afterCutoff), afterCutoff).getItems());
  }

  private static void ensureUser(String chatId) {
    TestUsers.ensureExists(
        PersistenceConfig.getDataSource(), Long.parseLong(chatId), TEST_USER_NAME);
  }

  private static String poolId(City city, LocalDate date) {
    return pool(city, date).getId();
  }

  private static SharedOrderItem entry(String entryId, String itemName, Category category) {
    return new SharedOrderItem(entryId, new Item(null, itemName, category), SOURCE_CHAT_ID, null);
  }

  private static SharedOrderItemPool pool(City city, LocalDate date, SharedOrderItem... entries) {
    SharedOrderItemPool pool = new SharedOrderItemPool();
    pool.setCity(city);
    pool.setDate(date);
    pool.getItems().addAll(List.of(entries));
    return pool;
  }

  private static Menu buildMenu(City city, LocalDate date, String itemName, Category category) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setDate(date.toString());
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(0, itemName, category)));
    menu.setAvailable(true);
    menu.setNotificated(false);
    return menu;
  }
}
