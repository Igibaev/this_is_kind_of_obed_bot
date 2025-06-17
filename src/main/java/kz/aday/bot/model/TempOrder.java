/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class TempOrder extends Order  {
  private String id;
  private Status status;
  private Set<Item> orderItemList = new HashSet<>(5);
  private Set<Category> categoryItemList = new HashSet<>(5);

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return id + ": " + orderItemList;
  }

}

