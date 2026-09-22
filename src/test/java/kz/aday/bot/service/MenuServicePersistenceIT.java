/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import kz.aday.bot.configuration.BotConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.JsonFileStorageSupport;
import kz.aday.bot.testsupport.AbstractPersistenceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MenuServicePersistenceIT extends AbstractPersistenceTest {

  private final MenuService cleanupService = new MenuService();

  @AfterEach
  void tearDown() {
    for (City city : City.values()) {
      cleanupService.deleteById(city.toString());
    }
  }

  @Test
  void save_thenFindById_returnsPersistedMenu() {
    Menu menu = buildMenu(City.ALMATA);
    MenuService service = new MenuService();

    service.save(menu);
    Menu found = service.findById(menu.getId());

    assertEquals(menu, found);
  }

  @Test
  void newServiceInstance_seesMenuSavedByPreviousInstance() {
    Menu menu = buildMenu(City.ASTANA);
    new MenuService().save(menu);

    Menu found = new MenuService().findById(menu.getId());

    assertEquals(menu, found);
  }

  @Test
  void deadline_survivesSaveAndReload_withSecondPrecision() {
    Menu menu = buildMenu(City.KARAGANDA);

    new MenuService().save(menu);
    Menu found = new MenuService().findById(menu.getId());

    assertEquals(menu.getDeadline(), found.getDeadline());
  }

  @Test
  void save_writesMenuUnderFixedStorageDate_soItSurvivesMidnightRollover() {
    Menu menu = buildMenu(City.ALMATA);
    MenuService service = new MenuService();

    service.save(menu);

    Path expectedFile =
        Path.of(
            BotConfig.getBotStorePath(),
            "menu",
            Menu.STORAGE_DATE.toString(),
            City.ALMATA + JsonFileStorageSupport.JSON);
    assertTrue(
        Files.exists(expectedFile),
        "Menu must be written under its fixed storage date, not under today's date, "
            + "otherwise it becomes unreachable the moment the calendar day changes.");
  }

  @Test
  void findById_ignoresStaleTodayDatedCopy_andStaysAnchoredToFixedStorageDate() throws IOException {
    Menu realMenu = buildMenu(City.KARAGANDA);
    realMenu.setMessage("real menu");
    MenuService service = new MenuService();
    service.save(realMenu);

    Menu staleDecoy = buildMenu(City.KARAGANDA);
    staleDecoy.setMessage("stale decoy written under today's date");
    Path todayFolder = Path.of(BotConfig.getBotStorePath(), "menu", LocalDate.now().toString());
    Files.createDirectories(todayFolder);
    ObjectMapper objectMapper = JsonFileStorageSupport.createObjectMapper();
    objectMapper.writeValue(
        todayFolder.resolve(City.KARAGANDA + JsonFileStorageSupport.JSON).toFile(), staleDecoy);

    Menu found = new MenuService().findById(City.KARAGANDA.toString());

    assertEquals("real menu", found.getMessage());
  }

  @Test
  void deleteById_removesMenuFromStorage() {
    Menu menu = buildMenu(City.ALMATA);
    MenuService service = new MenuService();
    service.save(menu);
    assertTrue(service.existsById(menu.getId()));

    service.deleteById(menu.getId());

    assertFalse(service.existsById(menu.getId()));
  }

  private static Menu buildMenu(City city) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(1, "Плов", Category.SECOND)));
    menu.setDeadline(LocalDateTime.now().plusHours(2).truncatedTo(ChronoUnit.SECONDS));
    menu.setAvailable(true);
    menu.setNotificated(false);
    menu.setMessage("Второе\nПлов\n\nДедлайн 18:00");
    return menu;
  }
}
