/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.JdbcOrderRepository;
import kz.aday.bot.repository.JsonFileStorageSupport;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OrderPostMigrationVerifierTest extends AbstractDbPersistenceTest {

  private static final LocalDate SAFE_DATE = LocalDate.of(2099, 6, 15);
  private static final Path ORDER_BASE_PATH = Path.of(BotConfig.getBotStorePath()).resolve("order");
  private static final ObjectMapper OBJECT_MAPPER = JsonFileStorageSupport.createObjectMapper();

  private final JdbcOrderRepository postgresRepository =
      new JdbcOrderRepository(PersistenceConfig.getDataSource());
  private final List<Path> writtenFiles = new ArrayList<>();
  private final List<String> savedChatIds = new ArrayList<>();

  @AfterEach
  void cleanUp() throws IOException {
    for (Path file : writtenFiles) {
      Files.deleteIfExists(file);
    }
    writtenFiles.clear();
    for (String chatId : savedChatIds) {
      postgresRepository.deleteById(chatId + "_" + SAFE_DATE, SAFE_DATE);
    }
    savedChatIds.clear();
  }

  @Test
  void verify_reportsNoDiscrepancies_forARecordMigratedByTheRealMigrator() throws IOException {
    String chatId = "982000001";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Migrated");
    writeRawFile(chatId, order(chatId, City.ALMATA, Status.READY));
    savedChatIds.add(chatId);

    OrderJsonToPostgresMigrator.main(new String[0]);
    OrderComparisonReport report =
        OrderPostMigrationVerifier.verify(ORDER_BASE_PATH, postgresRepository);

    assertFalse(report.getMissingIds().contains(id));
    assertFalse(report.getExtraIds().contains(id));
    assertTrue(report.getFieldMismatches().stream().noneMatch(m -> m.getId().equals(id)));
    assertFalse(report.getPerDateCountMismatches().containsKey(SAFE_DATE));
  }

  @Test
  void verify_reportsMissingIdAndCountMismatch_whenSourceRecordNeverReachedPostgres()
      throws IOException {
    String chatId = "982000002";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Unmigrated");
    writeRawFile(chatId, order(chatId, City.ALMATA, Status.READY));

    OrderComparisonReport report =
        OrderPostMigrationVerifier.verify(ORDER_BASE_PATH, postgresRepository);

    assertTrue(report.getMissingIds().contains(id));
    CountMismatch mismatch = report.getPerDateCountMismatches().get(SAFE_DATE);
    assertEquals(1, mismatch.getSourceCount());
    assertEquals(0, mismatch.getDestinationCount());
  }

  @Test
  void verify_reportsExtraIdAndCountMismatch_whenDestinationHasNoMatchingSource() {
    String chatId = "982000003";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Extra");
    postgresRepository.save(order(chatId, City.ALMATA, Status.READY));
    savedChatIds.add(chatId);

    OrderComparisonReport report =
        OrderPostMigrationVerifier.verify(ORDER_BASE_PATH, postgresRepository);

    assertTrue(report.getExtraIds().contains(id));
    CountMismatch mismatch = report.getPerDateCountMismatches().get(SAFE_DATE);
    assertEquals(0, mismatch.getSourceCount());
    assertEquals(1, mismatch.getDestinationCount());
  }

  @Test
  void verify_reportsFieldMismatches_whenCityAndStatusDifferFromDestination() throws IOException {
    String chatId = "982000004";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Diverged");
    writeRawFile(chatId, order(chatId, City.ALMATA, Status.READY));
    postgresRepository.save(order(chatId, City.ASTANA, Status.DEADLINE));
    savedChatIds.add(chatId);

    OrderComparisonReport report =
        OrderPostMigrationVerifier.verify(ORDER_BASE_PATH, postgresRepository);

    FieldMismatch cityMismatch = fieldMismatch(report, id, "city");
    assertEquals("ALMATA", cityMismatch.getSourceValue());
    assertEquals("ASTANA", cityMismatch.getDestinationValue());
    FieldMismatch statusMismatch = fieldMismatch(report, id, "status");
    assertEquals("READY", statusMismatch.getSourceValue());
    assertEquals("DEADLINE", statusMismatch.getDestinationValue());
  }

  @Test
  void verify_reportsItemSetMismatch_whenOrderItemsDifferFromDestination() throws IOException {
    String chatId = "982000005";
    String id = chatId + "_" + SAFE_DATE;
    TestUsers.ensureExists(
        PersistenceConfig.getDataSource(), Long.parseLong(chatId), "ItemDiverged");
    writeRawFile(chatId, order(chatId, City.ALMATA, Status.READY));
    Order destination = order(chatId, City.ALMATA, Status.READY);
    destination.getOrderItemList().clear();
    destination.getOrderItemList().add(new Item(null, "Другое блюдо", Category.SALAD));
    destination.getCategoryItemList().clear();
    destination.getCategoryItemList().add(Category.SALAD);
    postgresRepository.save(destination);
    savedChatIds.add(chatId);

    OrderComparisonReport report =
        OrderPostMigrationVerifier.verify(ORDER_BASE_PATH, postgresRepository);

    FieldMismatch itemsMismatch = fieldMismatch(report, id, "orderItemList");
    assertEquals("Плов", itemsMismatch.getSourceValue());
    assertEquals("Другое блюдо", itemsMismatch.getDestinationValue());
  }

  private static FieldMismatch fieldMismatch(
      OrderComparisonReport report, String id, String field) {
    return report.getFieldMismatches().stream()
        .filter(m -> m.getId().equals(id) && m.getField().equals(field))
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("Expected a [" + field + "] mismatch for id [" + id + "]"));
  }

  private void writeRawFile(String chatId, Order order) throws IOException {
    Path folder = ORDER_BASE_PATH.resolve(SAFE_DATE.toString());
    Files.createDirectories(folder);
    Path file = folder.resolve(chatId + ".json");
    OBJECT_MAPPER.writeValue(file.toFile(), order);
    writtenFiles.add(file);
  }

  private static Order order(String chatId, City city, Status status) {
    Order order = new Order();
    order.setChatId(chatId);
    order.setCity(city);
    order.setStatus(status);
    order.setDate(SAFE_DATE);
    order.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    order.getCategoryItemList().add(Category.SECOND);
    return order;
  }
}
