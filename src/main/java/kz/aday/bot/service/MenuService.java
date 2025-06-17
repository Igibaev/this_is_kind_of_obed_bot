/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.Menu;
import kz.aday.bot.repository.BaseRepository;

public class MenuService extends BaseService<Menu> {
  public MenuService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), Menu.class, "menu"));
  }
}
