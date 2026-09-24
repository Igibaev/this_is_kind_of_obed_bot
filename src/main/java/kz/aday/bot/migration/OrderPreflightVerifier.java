/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.migration.OrderSourceScanner.SourceEntry;
import kz.aday.bot.repository.JdbcUserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderPreflightVerifier {

  private static final String ORDER_STORAGE_PATH = "order";

  private OrderPreflightVerifier() {}

  public static void main(String[] args) {
    Path orderBasePath = Path.of(BotConfig.getBotStorePath()).resolve(ORDER_STORAGE_PATH);
    JdbcUserRepository userRepository = new JdbcUserRepository(PersistenceConfig.getDataSource());

    OrderPreflightReport report = verify(orderBasePath, userRepository);

    log.info(
        "Preflight found [{}] duplicate group(s), [{}] folder/date mismatch(es), [{}] orphan"
            + " chatId(s)",
        report.getDuplicateGroups().size(),
        report.getFolderDateMismatches().size(),
        report.getOrphanChatIds().size());
    report.getDuplicateGroups().forEach(group -> log.info("Duplicate group: {}", group));
    report
        .getFolderDateMismatches()
        .forEach(mismatch -> log.info("Folder/date mismatch: {}", mismatch));
    report.getOrphanChatIds().forEach(chatId -> log.warn("Orphan chatId: {}", chatId));
    log.info("hasBlockingIssues=[{}]", report.hasBlockingIssues());
  }

  public static OrderPreflightReport verify(Path orderBasePath, JdbcUserRepository userRepository) {
    List<SourceEntry> entries = OrderSourceScanner.scan(orderBasePath);

    List<DuplicateGroup> duplicateGroups = findDuplicateGroups(entries);
    List<DateMismatch> folderDateMismatches = findFolderDateMismatches(entries);
    List<String> orphanChatIds = findOrphanChatIds(entries, userRepository);

    return new OrderPreflightReport(duplicateGroups, folderDateMismatches, orphanChatIds);
  }

  private static List<DuplicateGroup> findDuplicateGroups(List<SourceEntry> entries) {
    Map<String, List<SourceEntry>> byEffectiveId =
        entries.stream().collect(Collectors.groupingBy(SourceEntry::effectiveId));

    List<DuplicateGroup> groups = new ArrayList<>();
    for (Map.Entry<String, List<SourceEntry>> entry : byEffectiveId.entrySet()) {
      List<SourceEntry> group = entry.getValue();
      if (group.size() < 2) {
        continue;
      }
      SourceEntry winner = OrderSourceScanner.pickWinner(group);
      List<Path> loserFiles =
          group.stream()
              .map(SourceEntry::getPath)
              .filter(path -> !path.equals(winner.getPath()))
              .toList();
      boolean conflict =
          group.stream()
              .anyMatch(
                  sourceEntry ->
                      !Objects.equals(sourceEntry.getCity(), winner.getCity())
                          || !Objects.equals(sourceEntry.getStatus(), winner.getStatus()));
      groups.add(new DuplicateGroup(entry.getKey(), winner.getPath(), loserFiles, conflict));
    }
    return groups;
  }

  private static List<DateMismatch> findFolderDateMismatches(List<SourceEntry> entries) {
    return entries.stream()
        .filter(
            sourceEntry ->
                sourceEntry.getFieldDate() != null
                    && !sourceEntry.getFieldDate().equals(sourceEntry.getFolderDate()))
        .map(
            sourceEntry ->
                new DateMismatch(
                    sourceEntry.effectiveId(),
                    sourceEntry.getFolderDate(),
                    sourceEntry.getFieldDate()))
        .toList();
  }

  private static List<String> findOrphanChatIds(
      List<SourceEntry> entries, JdbcUserRepository userRepository) {
    Set<String> knownChatIds =
        userRepository.getAll().stream()
            .map(user -> String.valueOf(user.getChatId()))
            .collect(Collectors.toSet());
    return entries.stream()
        .map(SourceEntry::getChatId)
        .distinct()
        .filter(chatId -> !knownChatIds.contains(chatId))
        .toList();
  }
}
