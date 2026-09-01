/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Menu implements Id {
  private City city;
  private Status status;
  private List<Item> itemList = new ArrayList<>();
  private LocalDateTime deadline;
  private Boolean available;
  private Boolean notificated;
  private String message;

  @Override
  public String getId() {
    return city.toString();
  }

  public Optional<Item> getItemById(Integer itemId) {
    return itemList.parallelStream().filter(item -> item.getId().equals(itemId)).findFirst();
  }

  public boolean isDeadlinePassed() {
    return LocalDateTime.now().isAfter(deadline);
  }

  public boolean isDeadlineNear() {
    return LocalDateTime.now().plusMinutes(10).isAfter(deadline);
  }

  public String getDeadlineAsText() {
    return deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
  }

  public String getMenuAsFormattedText() {
    Map<Category, List<Item>> byCategory = new LinkedHashMap<>();
    for (Item item : itemList) {
      byCategory.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
    }
    StringBuilder sb = new StringBuilder();
    byCategory.forEach(
        (category, items) -> {
          sb.append(category.getDisplayName()).append(":\n");
          items.forEach(item -> sb.append("• ").append(item.getName()).append("\n"));
          sb.append("\n");
        });
    return sb.toString().trim();
  }
}
