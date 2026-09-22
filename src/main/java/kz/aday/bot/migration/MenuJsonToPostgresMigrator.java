/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.time.LocalDate;
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
  private static final LocalDate LEGACY_STORAGE_DATE = LocalDate.of(2099, 1, 1);

  private static final LocalDate UNUSED_PRELOAD_DATE = LocalDate.of(1900, 1, 1);

  private MenuJsonToPostgresMigrator() {}

  public static void main(String[] args) {
    BaseRepository<Menu> jsonRepository =
        new BaseRepository<>(
            new ConcurrentHashMap<>(), Menu.class, MENU_STORAGE_PATH, UNUSED_PRELOAD_DATE);
    JdbcMenuRepository postgresRepository =
        new JdbcMenuRepository(PersistenceConfig.getDataSource());

    Collection<Menu> menus = jsonRepository.getAll(LEGACY_STORAGE_DATE);
    log.info("Migrating [{}] menu(s) from JSON storage to Postgres", menus.size());

    for (Menu menu : menus) {
      menu.setDate(menu.getCity().getCurrentOrderDate().toString());
      postgresRepository.save(menu);
    }

    log.info("Migration complete");
  }
}
