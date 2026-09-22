/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.BaseRepository;

public class MenuService extends BaseService<Menu> {
  public MenuService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), Menu.class, "menu", Menu.STORAGE_DATE));
  }

  @Override
  public Menu save(Menu entity) {
    Menu result = repository.getById(entity.getId(), entity.getStorageDate());
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

  @Override
  public Menu findById(String id) {
    return repository.getById(id, Menu.STORAGE_DATE);
  }

  @Override
  public Optional<Menu> findByIdOptional(String id) {
    return Optional.ofNullable(repository.getById(id, Menu.STORAGE_DATE));
  }

  @Override
  public boolean existsById(String id) {
    return repository.existById(id, Menu.STORAGE_DATE);
  }

  @Override
  public void deleteById(String id) {
    repository.deleteById(id, Menu.STORAGE_DATE);
  }

  @Override
  public Collection<Menu> findAll() {
    return repository.getAll(Menu.STORAGE_DATE);
  }
}
