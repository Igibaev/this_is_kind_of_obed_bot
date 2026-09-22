/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.JdbcMenuRepository;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MenuJsonToPostgresMigratorTest extends AbstractDbPersistenceTest {

  private static final LocalDate LEGACY_STORAGE_DATE = LocalDate.of(2099, 1, 1);

  private final BaseRepository<Menu> jsonRepository =
      new BaseRepository<>(new ConcurrentHashMap<>(), Menu.class, "menu", LEGACY_STORAGE_DATE);
  private final JdbcMenuRepository postgresRepository =
      new JdbcMenuRepository(PersistenceConfig.getDataSource());

  @AfterEach
  void cleanUpMigratedMenus() {
    jsonRepository.deleteById(City.ALMATA.toString(), LEGACY_STORAGE_DATE);
    jsonRepository.deleteById(City.ASTANA.toString(), LEGACY_STORAGE_DATE);
    jsonRepository.deleteById(City.KARAGANDA.toString(), LEGACY_STORAGE_DATE);
    postgresRepository.deleteById(City.ALMATA.toString(), City.ALMATA.getCurrentOrderDate());
    postgresRepository.deleteById(City.ASTANA.toString(), City.ASTANA.getCurrentOrderDate());
    postgresRepository.deleteById(City.KARAGANDA.toString(), City.KARAGANDA.getCurrentOrderDate());
  }

  @Test
  void main_migratesEveryMenuFromJsonStorageIntoPostgres_settingCurrentOrderDate() {
    Menu almaty = buildJsonMenu(City.ALMATA, "Плов", Category.SECOND);
    Menu astana = buildJsonMenu(City.ASTANA, "Борщ", Category.FIRST);
    jsonRepository.save(almaty);
    jsonRepository.save(astana);

    MenuJsonToPostgresMigrator.main(new String[0]);

    Menu migratedAlmaty =
        postgresRepository.getById(City.ALMATA.toString(), City.ALMATA.getCurrentOrderDate());
    Menu migratedAstana =
        postgresRepository.getById(City.ASTANA.toString(), City.ASTANA.getCurrentOrderDate());
    assertEquals("Плов", migratedAlmaty.getItemList().get(0).getName());
    assertEquals(Category.SECOND, migratedAlmaty.getItemList().get(0).getCategory());
    assertEquals("Борщ", migratedAstana.getItemList().get(0).getName());
    assertEquals(Category.FIRST, migratedAstana.getItemList().get(0).getCategory());
  }

  @Test
  void main_ignoresJsonFiles_underAnyFolderOtherThanTheLegacyStorageDate() {
    Menu staleTodayDatedCopy = buildJsonMenu(City.ASTANA, "Устаревшая копия", Category.SECOND);
    staleTodayDatedCopy.setDate(LocalDate.now().toString());
    BaseRepository<Menu> todayDatedJsonRepository =
        new BaseRepository<>(new ConcurrentHashMap<>(), Menu.class, "menu");
    todayDatedJsonRepository.save(staleTodayDatedCopy);

    try {
      MenuJsonToPostgresMigrator.main(new String[0]);

      Menu migrated =
          postgresRepository.getById(City.ASTANA.toString(), City.ASTANA.getCurrentOrderDate());
      assertNull(migrated, "Stale copies outside the legacy storage date must be ignored");
    } finally {
      todayDatedJsonRepository.deleteById(City.ASTANA.toString(), LocalDate.now());
    }
  }

  @Test
  void main_overwritesExistingRow_whenMenuWasAlreadyMigratedBefore() {
    Menu stale = new Menu();
    stale.setCity(City.KARAGANDA);
    stale.setDate(City.KARAGANDA.getCurrentOrderDate().toString());
    stale.setStatus(Status.READY);
    stale.setItemList(List.of(new Item(0, "Старое блюдо", Category.SECOND)));
    postgresRepository.save(stale);

    Menu fresh = buildJsonMenu(City.KARAGANDA, "Новое блюдо", Category.SECOND);
    jsonRepository.save(fresh);

    MenuJsonToPostgresMigrator.main(new String[0]);

    Menu migrated =
        postgresRepository.getById(City.KARAGANDA.toString(), City.KARAGANDA.getCurrentOrderDate());
    assertEquals(1, migrated.getItemList().size());
    assertEquals("Новое блюдо", migrated.getItemList().get(0).getName());
  }

  @Test
  void main_doesNothing_whenNoJsonMenusExist() {
    Menu before =
        postgresRepository.getById(City.ALMATA.toString(), City.ALMATA.getCurrentOrderDate());

    MenuJsonToPostgresMigrator.main(new String[0]);

    Menu after =
        postgresRepository.getById(City.ALMATA.toString(), City.ALMATA.getCurrentOrderDate());
    assertEquals(before, after);
  }

  private static Menu buildJsonMenu(City city, String itemName, Category category) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setDate(LEGACY_STORAGE_DATE.toString());
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(0, itemName, category)));
    menu.setDeadline(LocalDateTime.now().plusHours(2));
    return menu;
  }
}
