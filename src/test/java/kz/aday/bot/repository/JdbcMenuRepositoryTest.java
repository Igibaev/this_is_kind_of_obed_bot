/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import org.junit.jupiter.api.Test;

class JdbcMenuRepositoryTest extends AbstractDbPersistenceTest {

  private static final LocalDate DATE_1 = LocalDate.of(2031, 3, 1);
  private static final LocalDate DATE_2 = LocalDate.of(2031, 3, 2);
  private static final LocalDate DATE_3 = LocalDate.of(2031, 3, 3);
  private static final LocalDate DATE_4 = LocalDate.of(2031, 3, 4);
  private static final LocalDate DATE_5 = LocalDate.of(2031, 3, 5);
  private static final LocalDate DATE_6 = LocalDate.of(2031, 3, 6);
  private static final LocalDate DATE_7 = LocalDate.of(2031, 3, 7);
  private static final LocalDate DATE_8 = LocalDate.of(2031, 3, 8);
  private static final LocalDate DATE_9 = LocalDate.of(2031, 3, 9);
  private static final LocalDate DATE_10 = LocalDate.of(2031, 3, 10);
  private static final LocalDate DATE_11 = LocalDate.of(2031, 3, 11);

  private final JdbcMenuRepository repository =
      new JdbcMenuRepository(PersistenceConfig.getDataSource());

  @Test
  void getById_returnsNull_whenMenuWasNeverSaved() {
    assertNull(repository.getById(City.ALMATA.toString(), DATE_1));
  }

  @Test
  void existById_returnsFalse_whenMenuWasNeverSaved() {
    assertFalse(repository.existById(City.ALMATA.toString(), DATE_1));
  }

  @Test
  void save_thenGetById_roundTripsHeaderFieldsAndPreservesItemOrder() {
    Menu menu = buildMenu(City.ALMATA, DATE_2);
    menu.setItemList(
        List.of(
            new Item(0, "Борщ", Category.FIRST),
            new Item(1, "Хлеб", Category.BREAD),
            new Item(2, "Плов", Category.SECOND),
            new Item(3, "Компот", Category.BEVERAGE)));

    repository.save(menu);
    Menu found = repository.getById(City.ALMATA.toString(), DATE_2);

    assertEquals(menu, found);
    assertItemFieldsMatchInOrder(menu.getItemList(), found.getItemList());
  }

  @Test
  void save_calledTwiceForSameDate_replacesItems_insteadOfAppending() {
    Menu menu = buildMenu(City.ASTANA, DATE_3);
    menu.setItemList(
        List.of(new Item(0, "Борщ", Category.FIRST), new Item(1, "Плов", Category.SECOND)));
    repository.save(menu);

    Menu updated = buildMenu(City.ASTANA, DATE_3);
    updated.setItemList(List.of(new Item(0, "Салат", Category.SALAD)));
    repository.save(updated);

    Menu found = repository.getById(City.ASTANA.toString(), DATE_3);
    assertEquals(1, found.getItemList().size());
    assertEquals("Салат", found.getItemList().get(0).getName());
    assertEquals(Category.SALAD, found.getItemList().get(0).getCategory());
  }

  @Test
  void save_onNewDate_doesNotAffectMenuOnPreviousDate_forSameCity() {
    Menu oldMenu = buildMenu(City.KARAGANDA, DATE_4);
    oldMenu.setMessage("old menu");
    repository.save(oldMenu);

    Menu newMenu = buildMenu(City.KARAGANDA, DATE_5);
    newMenu.setMessage("new menu");
    repository.save(newMenu);

    Menu foundOld = repository.getById(City.KARAGANDA.toString(), DATE_4);
    Menu foundNew = repository.getById(City.KARAGANDA.toString(), DATE_5);
    assertEquals("old menu", foundOld.getMessage());
    assertEquals("new menu", foundNew.getMessage());
  }

  @Test
  void deleteById_removesOnlyThatDate_leavingOtherDatesForSameCityIntact() {
    Menu menu = buildMenu(City.ALMATA, DATE_6);
    repository.save(menu);
    Menu otherDateMenu = buildMenu(City.ALMATA, DATE_7);
    repository.save(otherDateMenu);

    repository.deleteById(City.ALMATA.toString(), DATE_6);

    assertNull(repository.getById(City.ALMATA.toString(), DATE_6));
    assertTrue(repository.existById(City.ALMATA.toString(), DATE_7));
  }

  @Test
  void getAllByDate_returnsMenusForEveryCity_onThatDate() {
    Menu almaty = buildMenu(City.ALMATA, DATE_8);
    Menu astana = buildMenu(City.ASTANA, DATE_8);
    repository.save(almaty);
    repository.save(astana);

    Collection<Menu> menus = repository.getAll(DATE_8);

    assertTrue(menus.contains(almaty));
    assertTrue(menus.contains(astana));
  }

  @Test
  void getAll_returnsEveryPreviouslySavedMenu_acrossDifferentDates() {
    Menu first = buildMenu(City.ALMATA, DATE_9);
    Menu second = buildMenu(City.ALMATA, DATE_10);
    repository.save(first);
    repository.save(second);

    Collection<Menu> menus = repository.getAll();

    assertTrue(menus.contains(first));
    assertTrue(menus.contains(second));
  }

  @Test
  void clearLastWeekAndClearStorage_doNotDeleteAnyData() {
    Menu menu = buildMenu(City.ASTANA, DATE_11);
    repository.save(menu);

    repository.clearLastWeek();
    repository.clearStorage();

    assertTrue(repository.existById(City.ASTANA.toString(), DATE_11));
  }

  private static void assertItemFieldsMatchInOrder(List<Item> expected, List<Item> actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      Item expectedItem = expected.get(i);
      Item actualItem = actual.get(i);
      assertEquals(expectedItem.getId(), actualItem.getId(), "id at index " + i);
      assertEquals(expectedItem.getName(), actualItem.getName(), "name at index " + i);
      assertEquals(expectedItem.getCategory(), actualItem.getCategory(), "category at index " + i);
    }
  }

  private static Menu buildMenu(City city, LocalDate date) {
    Menu menu = new Menu();
    menu.setCity(city);
    menu.setDate(date.toString());
    menu.setStatus(Status.READY);
    menu.setItemList(List.of(new Item(0, "Плов", Category.SECOND)));
    menu.setDeadline(LocalDateTime.of(date, LocalTime.of(18, 0)).truncatedTo(ChronoUnit.SECONDS));
    menu.setAvailable(true);
    menu.setNotificated(false);
    menu.setMessage("Второе\nПлов\n\nДедлайн 18:00");
    return menu;
  }
}
