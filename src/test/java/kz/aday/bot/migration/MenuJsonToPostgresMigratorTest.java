/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

  private static final LocalDate JSON_FOLDER_DATE_ALMATA = LocalDate.of(2099, 1, 11);
  private static final LocalDate JSON_FOLDER_DATE_ASTANA = LocalDate.of(2099, 1, 12);
  private static final LocalDate JSON_FOLDER_DATE_KARAGANDA = LocalDate.of(2099, 1, 13);

  private final BaseRepository<Menu> jsonRepository =
      new BaseRepository<>(new ConcurrentHashMap<>(), Menu.class, "menu");
  private final JdbcMenuRepository postgresRepository =
      new JdbcMenuRepository(PersistenceConfig.getDataSource());

  @AfterEach
  void cleanUpMigratedMenus() {
    jsonRepository.deleteById(City.ALMATA.toString(), JSON_FOLDER_DATE_ALMATA);
    jsonRepository.deleteById(City.ASTANA.toString(), JSON_FOLDER_DATE_ASTANA);
    jsonRepository.deleteById(City.KARAGANDA.toString(), JSON_FOLDER_DATE_KARAGANDA);
    postgresRepository.deleteById(City.ALMATA.toString(), City.ALMATA.getCurrentOrderDate());
    postgresRepository.deleteById(City.ASTANA.toString(), City.ASTANA.getCurrentOrderDate());
    postgresRepository.deleteById(City.KARAGANDA.toString(), City.KARAGANDA.getCurrentOrderDate());
  }

  @Test
  void main_migratesEveryMenuFromJsonStorageIntoPostgres_settingCurrentOrderDate() {
    Menu almaty = buildJsonMenu(City.ALMATA, JSON_FOLDER_DATE_ALMATA, "Плов", Category.SECOND);
    Menu astana = buildJsonMenu(City.ASTANA, JSON_FOLDER_DATE_ASTANA, "Борщ", Category.FIRST);
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
  void main_overwritesExistingRow_whenMenuWasAlreadyMigratedBefore() {
    Menu stale = new Menu();
    stale.setCity(City.KARAGANDA);
    stale.setDate(City.KARAGANDA.getCurrentOrderDate().toString());
    stale.setStatus(Status.READY);
    stale.setItemList(List.of(new Item(0, "Старое блюдо", Category.SECOND)));
    postgresRepository.save(stale);

    Menu fresh =
        buildJsonMenu(City.KARAGANDA, JSON_FOLDER_DATE_KARAGANDA, "Новое блюдо", Category.SECOND);
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

  private static Menu buildJsonMenu(
      City city, LocalDate jsonFolderDate, String itemName, Category category) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setDate(jsonFolderDate.toString());
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(0, itemName, category)));
    menu.setDeadline(LocalDateTime.now().plusHours(2));
    return menu;
  }
}
