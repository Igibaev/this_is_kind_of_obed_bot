/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.testsupport.AbstractPersistenceTest;
import org.junit.jupiter.api.Test;

class MenuServicePersistenceIT extends AbstractPersistenceTest {

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
