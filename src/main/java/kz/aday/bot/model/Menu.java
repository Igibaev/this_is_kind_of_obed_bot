/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    return LocalDateTime.now().minusMinutes(10).isAfter(deadline);
  }
}
