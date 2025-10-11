/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.MenuRules;

public class MenuRulesService {
  private static final Map<City, MenuRules> menuRulesMap = new HashMap<>();

  static {
    MenuRules astana = new MenuRules();
    Map<Category, Set<Category>> astanaRules = new HashMap<>();
    astanaRules.put(Category.FIRST, Set.of(Category.BAKERY, Category.SECOND));
    astanaRules.put(Category.SECOND, Set.of(Category.FIRST, Category.BAKERY));
    astanaRules.put(Category.BAKERY, Set.of(Category.SECOND, Category.FIRST));
    astana.setMenuRuleMap(astanaRules);
    menuRulesMap.put(City.ASTANA, astana);

    MenuRules almata = new MenuRules();
    Map<Category, Set<Category>> almataRules = new HashMap<>();
    almata.setMenuRuleMap(almataRules);
    menuRulesMap.put(City.ALMATA, almata);
  }

  public static MenuRules getMenuRule(City city) {
    return menuRulesMap.get(city);
  }
}
