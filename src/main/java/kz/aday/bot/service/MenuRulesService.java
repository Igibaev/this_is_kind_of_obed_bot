/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.HashMap;
import kz.aday.bot.model.MenuRules;
import kz.aday.bot.repository.BaseRepository;

public class MenuRulesService extends BaseService<MenuRules> {

  public MenuRulesService() {
    super(new BaseRepository<>(new HashMap<>(), MenuRules.class, "menu-rules"));
  }
}
