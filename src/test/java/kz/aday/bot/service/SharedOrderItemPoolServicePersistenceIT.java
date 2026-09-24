/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SharedOrderItemPoolServicePersistenceIT extends AbstractDbPersistenceTest {

  private static final LocalDate BASE_DATE = LocalDate.of(2099, 8, 1);
  private static final String SHARER_CHAT_ID = "958000001";
  private static final String FIRST_CLAIMER_CHAT_ID = "958000002";
  private static final String SECOND_CLAIMER_CHAT_ID = "958000003";
  private static final String SHARER_USERNAME = "sharer";
  private static final String FIRST_CLAIMER_USERNAME = "first-claimer";
  private static final String SECOND_CLAIMER_USERNAME = "second-claimer";
  private static final Item PLOV = new Item(null, "Плов", Category.SECOND);
  private static final Item SALAD = new Item(null, "Салат", Category.SALAD);

  @BeforeAll
  static void createUsers() {
    ensureUser(SHARER_CHAT_ID, SHARER_USERNAME);
    ensureUser(FIRST_CLAIMER_CHAT_ID, FIRST_CLAIMER_USERNAME);
    ensureUser(SECOND_CLAIMER_CHAT_ID, SECOND_CLAIMER_USERNAME);
  }

  @Test
  void addItems_thenNewServiceInstance_seesAvailableEntries() {
    LocalDate date = BASE_DATE;
    new SharedOrderItemPoolService()
        .addItems(City.ALMATA, date, SHARER_CHAT_ID, SHARER_USERNAME, List.of(PLOV, SALAD));

    List<SharedOrderItem> available =
        new SharedOrderItemPoolService().getAvailableEntries(City.ALMATA, date);

    assertEquals(List.of(PLOV, SALAD), available.stream().map(SharedOrderItem::getItem).toList());
    assertTrue(available.stream().allMatch(e -> SHARER_CHAT_ID.equals(e.getSourceChatId())));
  }

  @Test
  void addItems_calledTwice_keepsEntriesFromBothCalls() {
    LocalDate date = BASE_DATE.plusDays(1);
    SharedOrderItemPoolService service = new SharedOrderItemPoolService();
    service.addItems(City.ALMATA, date, SHARER_CHAT_ID, SHARER_USERNAME, List.of(PLOV));

    service.addItems(City.ALMATA, date, SHARER_CHAT_ID, SHARER_USERNAME, List.of(SALAD));

    List<SharedOrderItem> available =
        new SharedOrderItemPoolService().getAvailableEntries(City.ALMATA, date);
    assertEquals(List.of(PLOV, SALAD), available.stream().map(SharedOrderItem::getItem).toList());
  }

  @Test
  void claim_persistsClaim_andHidesEntryFromAvailable() {
    LocalDate date = BASE_DATE.plusDays(2);
    SharedOrderItemPoolService service = new SharedOrderItemPoolService();
    service.addItems(City.ALMATA, date, SHARER_CHAT_ID, SHARER_USERNAME, List.of(PLOV));
    String entryId = service.getAvailableEntries(City.ALMATA, date).get(0).getEntryId();

    Optional<SharedOrderItem> claimed =
        service.claim(City.ALMATA, date, entryId, FIRST_CLAIMER_CHAT_ID, FIRST_CLAIMER_USERNAME);

    assertTrue(claimed.isPresent());
    assertTrue(new SharedOrderItemPoolService().getAvailableEntries(City.ALMATA, date).isEmpty());
  }

  @Test
  void claim_returnsEmpty_whenEntryWasAlreadyClaimedByAnotherInstance() {
    LocalDate date = BASE_DATE.plusDays(3);
    new SharedOrderItemPoolService()
        .addItems(City.ALMATA, date, SHARER_CHAT_ID, SHARER_USERNAME, List.of(PLOV));
    String entryId =
        new SharedOrderItemPoolService().getAvailableEntries(City.ALMATA, date).get(0).getEntryId();
    new SharedOrderItemPoolService()
        .claim(City.ALMATA, date, entryId, FIRST_CLAIMER_CHAT_ID, FIRST_CLAIMER_USERNAME);

    Optional<SharedOrderItem> secondClaim =
        new SharedOrderItemPoolService()
            .claim(City.ALMATA, date, entryId, SECOND_CLAIMER_CHAT_ID, SECOND_CLAIMER_USERNAME);

    assertTrue(secondClaim.isEmpty());
  }

  @Test
  void getAvailableEntries_doesNotReturnEntriesOfAnotherCityOrDate() {
    LocalDate date = BASE_DATE.plusDays(4);
    SharedOrderItemPoolService service = new SharedOrderItemPoolService();
    service.addItems(City.ASTANA, date, SHARER_CHAT_ID, SHARER_USERNAME, List.of(PLOV));
    service.addItems(
        City.ALMATA, date.plusDays(1), SHARER_CHAT_ID, SHARER_USERNAME, List.of(SALAD));

    assertTrue(service.getAvailableEntries(City.ALMATA, date).isEmpty());
  }

  private static void ensureUser(String chatId, String preferedName) {
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), preferedName);
  }
}
