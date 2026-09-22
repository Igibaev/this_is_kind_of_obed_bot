/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.JdbcMenuRepository;
import kz.aday.bot.repository.Repository;

public class MenuService extends BaseService<Menu> {
  public MenuService() {
    super(new JdbcMenuRepository(PersistenceConfig.getDataSource()));
  }

  MenuService(Repository<Menu> repository) {
    super(repository);
  }

  @Override
  public Menu save(Menu entity) {
    if (entity.getDate() == null) {
      entity.setDate(entity.getCity().getCurrentOrderDate().toString());
    }
    Menu existing = findById(entity.getId());
    if (existing != null && existing.getStatus() == Status.DEADLINE) {
      deleteById(existing.getId());
    }
    return super.save(entity);
  }

  @Override
  public Menu findById(String id) {
    return repository.getById(id, City.valueOf(id).getCurrentOrderDate());
  }

  @Override
  public Optional<Menu> findByIdOptional(String id) {
    return Optional.ofNullable(findById(id));
  }

  @Override
  public boolean existsById(String id) {
    return repository.existById(id, City.valueOf(id).getCurrentOrderDate());
  }

  @Override
  public void deleteById(String id) {
    repository.deleteById(id, City.valueOf(id).getCurrentOrderDate());
  }

  @Override
  public Collection<Menu> findAll() {
    List<Menu> menus = new ArrayList<>();
    for (City city : City.values()) {
      Menu menu = findById(city.toString());
      if (menu != null) {
        menus.add(menu);
      }
    }
    return menus;
  }
}
