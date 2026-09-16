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
    return city + "_" + date;
  }
}
