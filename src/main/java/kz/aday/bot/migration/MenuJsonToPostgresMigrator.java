/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Menu;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.JdbcMenuRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MenuJsonToPostgresMigrator {

  private static final String MENU_STORAGE_PATH = "menu";

  private MenuJsonToPostgresMigrator() {}

  public static void main(String[] args) {
    BaseRepository<Menu> jsonRepository =
        new BaseRepository<>(new ConcurrentHashMap<>(), Menu.class, MENU_STORAGE_PATH);
    JdbcMenuRepository postgresRepository =
        new JdbcMenuRepository(PersistenceConfig.getDataSource());

    Collection<Menu> menus = jsonRepository.getAll();
    log.info("Migrating [{}] menu(s) from JSON storage to Postgres", menus.size());

    for (Menu menu : menus) {
      menu.setDate(menu.getCity().getCurrentOrderDate().toString());
      postgresRepository.save(menu);
    }

    log.info("Migration complete");
  }
}
