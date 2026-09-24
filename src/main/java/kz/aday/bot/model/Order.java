/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import static kz.aday.bot.util.StringUtils.escapeMarkdown;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
public class Order implements Id {
  private String chatId;
  private String username;
  private City city;
  private Status status;
  private LocalDate date;
  private LocalDateTime submittedAt;
  private Set<Item> orderItemList = new HashSet<>(5);
  private Set<Category> categoryItemList = new HashSet<>(5);

  @Override
  public String toString() {
    return String.format(
        "*%s*: [%s]", escapeMarkdown(username), StringUtils.join(orderItemList, ","));
  }

  @Override
  public String getId() {
    return chatId + "_" + date;
  }

  @Override
  public LocalDate getStorageDate() {
    return date;
  }

  @Override
  public void backfillDateIfMissing(LocalDate folderDate) {
    if (date == null) {
      date = folderDate;
    }
  }
}
