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
import kz.aday.bot.repository.JdbcUserRepository;
import kz.aday.bot.repository.JsonFileStorageSupport;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OrderPreflightVerifierTest extends AbstractDbPersistenceTest {

  private static final LocalDate TODAY = LocalDate.now();
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);
  private static final Path ORDER_BASE_PATH = Path.of(BotConfig.getBotStorePath()).resolve("order");
  private static final ObjectMapper OBJECT_MAPPER = JsonFileStorageSupport.createObjectMapper();

  private final JdbcUserRepository userRepository =
      new JdbcUserRepository(PersistenceConfig.getDataSource());
  private final List<Path> writtenFiles = new ArrayList<>();

  @AfterEach
  void cleanUpWrittenFiles() throws IOException {
    for (Path file : writtenFiles) {
      Files.deleteIfExists(file);
    }
    writtenFiles.clear();
  }

  @Test
  void verify_reportsNoIssues_forAWellFormedRecordWithAKnownUser() throws IOException {
    String chatId = "981000001";
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Clean");
    writeRawFile(TODAY, "clean", order(chatId, City.ALMATA, Status.READY, TODAY));
    String id = chatId + "_" + TODAY;

    OrderPreflightReport report = OrderPreflightVerifier.verify(ORDER_BASE_PATH, userRepository);

    assertTrue(report.getDuplicateGroups().stream().noneMatch(group -> group.getId().equals(id)));
    assertTrue(report.getFolderDateMismatches().stream().noneMatch(m -> m.getId().equals(id)));
    assertFalse(report.getOrphanChatIds().contains(chatId));
  }

  @Test
  void verify_marksDuplicateAsNonConflict_whenBothFilesAgreeOnValues() throws IOException {
    String chatId = "981000002";
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Redundant");
    writeRawFile(YESTERDAY, "before", order(chatId, City.ALMATA, Status.READY, TODAY));
    writeRawFile(TODAY, "sameday", order(chatId, City.ALMATA, Status.READY, TODAY));
    String id = chatId + "_" + TODAY;

    OrderPreflightReport report = OrderPreflightVerifier.verify(ORDER_BASE_PATH, userRepository);

    DuplicateGroup group = findGroup(report, id);
    assertFalse(group.isConflict());
  }

  @Test
  void verify_marksDuplicateAsConflict_andPicksSameDayFileAsWinner_whenValuesDisagree()
      throws IOException {
    String chatId = "981000003";
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Conflict");
    writeRawFile(YESTERDAY, "before", order(chatId, City.ALMATA, Status.READY, TODAY));
    Path sameDayFile =
        writeRawFile(TODAY, "sameday", order(chatId, City.ASTANA, Status.READY, TODAY));
    String id = chatId + "_" + TODAY;

    OrderPreflightReport report = OrderPreflightVerifier.verify(ORDER_BASE_PATH, userRepository);

    DuplicateGroup group = findGroup(report, id);
    assertTrue(group.isConflict());
    assertEquals(sameDayFile, group.getWinnerFile());
    assertTrue(report.hasBlockingIssues());
  }

  @Test
  void verify_reportsFolderDateMismatch_whenFieldDateDisagreesWithFolder_andDoesNotBlock()
      throws IOException {
    String chatId = "981000004";
    TestUsers.ensureExists(PersistenceConfig.getDataSource(), Long.parseLong(chatId), "Mismatch");
    writeRawFile(YESTERDAY, "misfiled", order(chatId, City.ASTANA, Status.READY, TODAY));
    String id = chatId + "_" + TODAY;

    OrderPreflightReport report = OrderPreflightVerifier.verify(ORDER_BASE_PATH, userRepository);

    DateMismatch mismatch =
        report.getFolderDateMismatches().stream()
            .filter(m -> m.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a folder/date mismatch for " + id));
    assertEquals(YESTERDAY, mismatch.getFolderDate());
    assertEquals(TODAY, mismatch.getFieldDate());
    assertTrue(report.getDuplicateGroups().stream().noneMatch(group -> group.getId().equals(id)));
    assertFalse(report.getOrphanChatIds().contains(chatId));
  }

  @Test
  void verify_reportsOrphanChatId_andBlocks_whenChatIdHasNoUsersRow() throws IOException {
    String chatId = "981000005";
    writeRawFile(TODAY, "orphan", order(chatId, City.KARAGANDA, Status.READY, TODAY));

    OrderPreflightReport report = OrderPreflightVerifier.verify(ORDER_BASE_PATH, userRepository);

    assertTrue(report.getOrphanChatIds().contains(chatId));
    assertTrue(report.hasBlockingIssues());
  }

  private static DuplicateGroup findGroup(OrderPreflightReport report, String id) {
    return report.getDuplicateGroups().stream()
        .filter(group -> group.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No duplicate group found for id " + id));
  }

  private Path writeRawFile(LocalDate folderDate, String filename, Order order) throws IOException {
    Path folder = ORDER_BASE_PATH.resolve(folderDate.toString());
    Files.createDirectories(folder);
    Path file = folder.resolve(filename + ".json");
    OBJECT_MAPPER.writeValue(file.toFile(), order);
    writtenFiles.add(file);
    return file;
  }

  private static Order order(String chatId, City city, Status status, LocalDate date) {
    Order order = new Order();
    order.setChatId(chatId);
    order.setCity(city);
    order.setStatus(status);
    order.setDate(date);
    order.getOrderItemList().add(new Item(1, "Плов", Category.SECOND));
    order.getCategoryItemList().add(Category.SECOND);
    return order;
  }
}
