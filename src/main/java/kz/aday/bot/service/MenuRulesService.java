/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.EnumMap;
import java.util.Map;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.MenuRules;

public class MenuRulesService {
  private static final Map<City, MenuRules> MENU_RULES_BY_CITY = new EnumMap<>(City.class);

  static {
    for (City city : City.values()) {
      MENU_RULES_BY_CITY.put(city, new MenuRules(city, new EnumMap<>(Category.class)));
    }
  }

  private MenuRulesService() {}

  public static MenuRules getMenuRule(City city) {
    return MENU_RULES_BY_CITY.get(city);
  }
}
