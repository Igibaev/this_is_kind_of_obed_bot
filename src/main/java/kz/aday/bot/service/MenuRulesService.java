/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.HashMap;
import java.util.Map;
import kz.aday.bot.model.City;
import kz.aday.bot.model.MenuRules;

public class MenuRulesService {
  private static final Map<City, MenuRules> menuRulesMap = new HashMap<>();

  static {
    for (City city : City.values()) {
      MenuRules menuRules = new MenuRules();
      menuRulesMap.put(city, menuRules);
    }
  }

  public static MenuRules getMenuRule(City city) {
    return menuRulesMap.get(city);
  }
}
