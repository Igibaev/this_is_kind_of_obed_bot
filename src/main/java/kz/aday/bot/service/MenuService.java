/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.BaseRepository;

public class MenuService extends BaseService<Menu> {
  public MenuService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), Menu.class, "menu"));
  }

  @Override
  public Menu save(Menu entity) {
    Menu result = repository.getById(entity.getId());
    if (result == null) {
      return super.save(entity);
    } else {
      if (result.getStatus() == Status.DEADLINE) {
        deleteById(result.getId());
        return super.save(entity);
      }
    }
    return super.save(entity);
  }
}
