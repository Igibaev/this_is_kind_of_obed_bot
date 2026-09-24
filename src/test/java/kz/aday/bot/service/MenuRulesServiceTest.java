/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kz.aday.bot.model.City;
import kz.aday.bot.model.MenuRules;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MenuRulesServiceTest {

  @ParameterizedTest
  @EnumSource(City.class)
  void getMenuRule_returnsRulesBoundToRequestedCity(City city) {
    MenuRules menuRules = MenuRulesService.getMenuRule(city);

    assertEquals(city, menuRules.getCity());
    assertEquals(city.toString(), menuRules.getId());
  }

  @ParameterizedTest
  @EnumSource(City.class)
  void getMenuRule_hasNoDisjointCategories_byDefault(City city) {
    assertTrue(MenuRulesService.getMenuRule(city).getMenuRuleMap().isEmpty());
  }
}
