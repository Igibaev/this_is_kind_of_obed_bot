/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.MenuRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MenuService {
  private final MenuRepository repository;

  public MenuService() {
    this(new MenuRepository(PersistenceConfig.getDataSource()));
  }

  MenuService(MenuRepository repository) {
    this.repository = repository;
  }

  public Menu save(Menu menu) {
    if (menu.getDate() == null) {
      menu.setDate(menu.getCity().getCurrentOrderDate().toString());
    }
    Menu existing = findById(menu.getId());
    if (existing != null && existing.getStatus() == Status.DEADLINE) {
      deleteById(existing.getId());
    }
    repository.save(menu);
    log.info("Saved menu with ID: {}", menu.getId());
    return menu;
  }

  public Menu findById(String id) {
    return repository.getById(id, currentOrderDate(id));
  }

  public Optional<Menu> findByIdOptional(String id) {
    return Optional.ofNullable(findById(id));
  }

  public boolean existsById(String id) {
    return repository.existById(id, currentOrderDate(id));
  }

  public void deleteById(String id) {
    repository.deleteById(id, currentOrderDate(id));
    log.warn("Menu with ID {} was deleted.", id);
  }

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

  public void deleteOutdated() {
    repository.deleteBefore(LocalDate.now());
  }

  private static LocalDate currentOrderDate(String id) {
    return City.valueOf(id).getCurrentOrderDate();
  }
}
