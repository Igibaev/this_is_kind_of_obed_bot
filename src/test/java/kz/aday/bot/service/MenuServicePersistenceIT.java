/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.JdbcMenuRepository;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MenuServicePersistenceIT extends AbstractDbPersistenceTest {

  private static final LocalDate HISTORICAL_DATE_1 = LocalDate.of(2020, 1, 1);
  private static final LocalDate HISTORICAL_DATE_2 = LocalDate.of(2020, 1, 2);
  private static final LocalDate HISTORICAL_DATE_3 = LocalDate.of(2020, 1, 3);

  private final MenuService cleanupService = new MenuService();
  private final JdbcMenuRepository rawRepository =
      new JdbcMenuRepository(PersistenceConfig.getDataSource());

  @AfterEach
  void tearDown() {
    for (City city : City.values()) {
      cleanupService.deleteById(city.toString());
    }
    rawRepository.deleteById(City.ALMATA.toString(), HISTORICAL_DATE_1);
    rawRepository.deleteById(City.ASTANA.toString(), HISTORICAL_DATE_2);
    rawRepository.deleteById(City.KARAGANDA.toString(), HISTORICAL_DATE_3);
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
  void deleteById_removesMenuFromStorage() {
    Menu menu = buildMenu(City.ALMATA);
    MenuService service = new MenuService();
    service.save(menu);
    assertTrue(service.existsById(menu.getId()));

    service.deleteById(menu.getId());

    assertFalse(service.existsById(menu.getId()));
  }

  @Test
  void save_deletesExistingDeadlineMenu_beforeSavingReplacement() {
    MenuService service = new MenuService();
    Menu deadlineMenu = buildMenu(City.ASTANA);
    deadlineMenu.setStatus(Status.DEADLINE);
    deadlineMenu.setItemList(List.of(new Item(0, "Старое блюдо", Category.SECOND)));
    service.save(deadlineMenu);

    Menu replacement = buildMenu(City.ASTANA);
    replacement.setItemList(List.of(new Item(0, "Новое блюдо", Category.FIRST)));
    service.save(replacement);

    Menu found = service.findById(City.ASTANA.toString());
    assertEquals(1, found.getItemList().size());
    assertEquals("Новое блюдо", found.getItemList().get(0).getName());
    assertEquals(Category.FIRST, found.getItemList().get(0).getCategory());
    assertEquals(Status.READY, found.getStatus());
  }

  @Test
  void save_onNewOrderCycle_doesNotOverwritePreviousDatesMenu() {
    Menu historical = buildMenu(City.ALMATA);
    historical.setDate(HISTORICAL_DATE_1.toString());
    historical.setMessage("historical menu, must survive");
    rawRepository.save(historical);

    Menu current = buildMenu(City.ALMATA);
    current.setMessage("current menu");
    new MenuService().save(current);

    Menu stillThere = rawRepository.getById(City.ALMATA.toString(), HISTORICAL_DATE_1);
    assertEquals("historical menu, must survive", stillThere.getMessage());
    assertEquals("current menu", new MenuService().findById(City.ALMATA.toString()).getMessage());
  }

  @Test
  void findAll_returnsOnlyCurrentMenuPerCity_evenWhenHistoricalRowsExist() {
    Menu historicalAstana = buildMenu(City.ASTANA);
    historicalAstana.setDate(HISTORICAL_DATE_2.toString());
    rawRepository.save(historicalAstana);
    Menu historicalKaraganda = buildMenu(City.KARAGANDA);
    historicalKaraganda.setDate(HISTORICAL_DATE_3.toString());
    rawRepository.save(historicalKaraganda);

    Menu currentAlmata = buildMenu(City.ALMATA);
    MenuService service = new MenuService();
    service.save(currentAlmata);

    List<Menu> all = List.copyOf(service.findAll());

    assertEquals(1, all.size());
    assertEquals(City.ALMATA, all.get(0).getCity());
  }

  private static Menu buildMenu(City city) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(0, "Плов", Category.SECOND)));
    menu.setDeadline(LocalDateTime.now().plusHours(2).truncatedTo(ChronoUnit.SECONDS));
    menu.setAvailable(true);
    menu.setNotificated(false);
    menu.setMessage("Второе\nПлов\n\nДедлайн 18:00");
    return menu;
  }
}
