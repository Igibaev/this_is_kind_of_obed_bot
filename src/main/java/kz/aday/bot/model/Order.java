/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
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
  private Set<Item> orderItemList = new HashSet<>(5);
  private Set<Category> categoryItemList = new HashSet<>(5);

  @Override
  public String toString() {
    return String.format("*%s*: [%s]", username, StringUtils.join(orderItemList, ","));
  }

  @Override
  public String getId() {
    return chatId;
  }

}
