/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.migration.OrderSourceScanner.SourceEntry;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.repository.JdbcOrderRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderPostMigrationVerifier {

  private static final String ORDER_STORAGE_PATH = "order";
  private static final String FIELD_CITY = "city";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_DATE = "date";
  private static final String FIELD_ORDER_ITEMS = "orderItemList";
  private static final String FIELD_CATEGORIES = "categoryItemList";

  private OrderPostMigrationVerifier() {}

  public static void main(String[] args) {
    Path orderBasePath = Path.of(BotConfig.getBotStorePath()).resolve(ORDER_STORAGE_PATH);
    JdbcOrderRepository postgresRepository =
        new JdbcOrderRepository(PersistenceConfig.getDataSource());

    OrderComparisonReport report = verify(orderBasePath, postgresRepository);

    log.info(
        "Source has [{}] record(s), destination has [{}] record(s)",
        report.getSourceCount(),
        report.getDestinationCount());
    report
        .getPerDateCountMismatches()
        .values()
        .forEach(mismatch -> log.warn("Count mismatch: {}", mismatch));
    report.getMissingIds().forEach(id -> log.warn("Missing in destination: {}", id));
    report.getExtraIds().forEach(id -> log.warn("Extra in destination: {}", id));
    report.getFieldMismatches().forEach(mismatch -> log.warn("Field mismatch: {}", mismatch));
    log.info("isMatch=[{}]", report.isMatch());
  }

  public static OrderComparisonReport verify(
      Path orderBasePath, JdbcOrderRepository postgresRepository) {
    List<SourceEntry> entries = OrderSourceScanner.scan(orderBasePath);
    Map<String, Order> source = resolveSourceRecords(entries);
    Map<String, Order> destination =
        postgresRepository.getAll().stream()
            .collect(Collectors.toMap(Order::getId, order -> order));

    Map<LocalDate, CountMismatch> perDateCountMismatches =
        findPerDateCountMismatches(source, destination);
    List<String> missingIds =
        source.keySet().stream().filter(id -> !destination.containsKey(id)).sorted().toList();
    List<String> extraIds =
        destination.keySet().stream().filter(id -> !source.containsKey(id)).sorted().toList();
    List<FieldMismatch> fieldMismatches = findFieldMismatches(source, destination);

    return new OrderComparisonReport(
        source.size(),
        destination.size(),
        perDateCountMismatches,
        missingIds,
        extraIds,
        fieldMismatches);
  }

  private static Map<String, Order> resolveSourceRecords(List<SourceEntry> entries) {
    Map<String, List<SourceEntry>> byEffectiveId =
        entries.stream().collect(Collectors.groupingBy(SourceEntry::effectiveId));
    Map<String, Order> resolved = new HashMap<>();
    for (Map.Entry<String, List<SourceEntry>> entry : byEffectiveId.entrySet()) {
      SourceEntry winner = OrderSourceScanner.pickWinner(entry.getValue());
      Order order = new Order();
      order.setChatId(winner.getChatId());
      order.setCity(winner.getCity());
      order.setStatus(winner.getStatus());
      order.setDate(winner.getEffectiveDate());
      order.setSubmittedAt(winner.getSubmittedAt());
      order.setOrderItemList(winner.getOrderItemList());
      order.setCategoryItemList(winner.getCategoryItemList());
      resolved.put(entry.getKey(), order);
    }
    return resolved;
  }

  private static Map<LocalDate, CountMismatch> findPerDateCountMismatches(
      Map<String, Order> source, Map<String, Order> destination) {
    Map<LocalDate, Long> sourceCountsByDate =
        source.values().stream()
            .collect(Collectors.groupingBy(Order::getDate, Collectors.counting()));
    Map<LocalDate, Long> destinationCountsByDate =
        destination.values().stream()
            .collect(Collectors.groupingBy(Order::getDate, Collectors.counting()));

    Set<LocalDate> allDates = new HashSet<>(sourceCountsByDate.keySet());
    allDates.addAll(destinationCountsByDate.keySet());

    Map<LocalDate, CountMismatch> mismatches = new HashMap<>();
    for (LocalDate date : allDates) {
      long sourceCount = sourceCountsByDate.getOrDefault(date, 0L);
      long destinationCount = destinationCountsByDate.getOrDefault(date, 0L);
      if (sourceCount != destinationCount) {
        mismatches.put(date, new CountMismatch(date, (int) sourceCount, (int) destinationCount));
      }
    }
    return mismatches;
  }

  private static List<FieldMismatch> findFieldMismatches(
      Map<String, Order> source, Map<String, Order> destination) {
    List<FieldMismatch> mismatches = new ArrayList<>();
    for (Map.Entry<String, Order> entry : source.entrySet()) {
      Order sourceOrder = entry.getValue();
      Order destinationOrder = destination.get(entry.getKey());
      if (destinationOrder == null) {
        continue;
      }
      compareField(
          entry.getKey(),
          FIELD_CITY,
          String.valueOf(sourceOrder.getCity()),
          String.valueOf(destinationOrder.getCity()),
          mismatches);
      compareField(
          entry.getKey(),
          FIELD_STATUS,
          String.valueOf(sourceOrder.getStatus()),
          String.valueOf(destinationOrder.getStatus()),
          mismatches);
      compareField(
          entry.getKey(),
          FIELD_DATE,
          String.valueOf(sourceOrder.getDate()),
          String.valueOf(destinationOrder.getDate()),
          mismatches);
      compareField(
          entry.getKey(),
          FIELD_ORDER_ITEMS,
          itemNames(sourceOrder.getOrderItemList()),
          itemNames(destinationOrder.getOrderItemList()),
          mismatches);
      compareField(
          entry.getKey(),
          FIELD_CATEGORIES,
          categoryNames(sourceOrder.getCategoryItemList()),
          categoryNames(destinationOrder.getCategoryItemList()),
          mismatches);
    }
    return mismatches;
  }

  private static String itemNames(Set<Item> items) {
    return items.stream().map(Item::getName).sorted().collect(Collectors.joining(","));
  }

  private static String categoryNames(Set<Category> categories) {
    return categories.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
  }

  private static void compareField(
      String id,
      String field,
      String sourceValue,
      String destinationValue,
      List<FieldMismatch> mismatches) {
    if (!Objects.equals(sourceValue, destinationValue)) {
      mismatches.add(new FieldMismatch(id, field, sourceValue, destinationValue));
    }
  }
}
