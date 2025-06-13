/* (C) 2024 Igibaev */
package kz.aday.bot.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuRules implements Id {
  private City city;
  private Map<Category, Set<Category>> menuRuleMap = new HashMap<>();
  private Category choosenCategory;

  public MenuRules(City city, Map<Category, Set<Category>> menuRuleMap) {
    this.city = city;
    this.menuRuleMap = menuRuleMap;
  }

  @Override
  public String getId() {
    return city.toString();
  }
}
