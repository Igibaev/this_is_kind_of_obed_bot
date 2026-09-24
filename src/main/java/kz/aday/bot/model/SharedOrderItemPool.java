/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SharedOrderItemPool implements Id {
  private City city;
  private LocalDate date;
  private List<SharedOrderItem> items = new ArrayList<>();

  @Override
  public String getId() {
    return buildId(city, date);
  }

  public static String buildId(City city, LocalDate date) {
    return Id.composeId(city, date);
  }

  @Override
  public LocalDate getStorageDate() {
    return date;
  }
}
