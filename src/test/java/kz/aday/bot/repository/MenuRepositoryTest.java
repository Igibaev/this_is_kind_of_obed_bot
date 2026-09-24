/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import org.junit.jupiter.api.Test;

class MenuRepositoryTest extends AbstractDbPersistenceTest {

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
  private static final LocalDate DATE_12 = LocalDate.of(2031, 3, 12);
  private static final LocalDate DATE_13 = LocalDate.of(2031, 3, 13);
  private static final LocalDate DELETION_CUTOFF = LocalDate.of(1985, 6, 1);
  private static final LocalDate BEFORE_DELETION_CUTOFF = DELETION_CUTOFF.minusDays(1);

  private final MenuRepository repository = new MenuRepository(PersistenceConfig.getDataSource());

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
  void save_calledTwiceForSameDate_reusesItemId_forItemThatStillExistsByName() throws SQLException {
    Menu menu = buildMenu(City.KARAGANDA, DATE_12);
    menu.setItemList(
        List.of(new Item(0, "Плов", Category.SECOND), new Item(1, "Салат", Category.SALAD)));
    repository.save(menu);
    long plovIdBeforeEdit = findItemId(City.KARAGANDA, DATE_12, "Плов");

    Menu edited = buildMenu(City.KARAGANDA, DATE_12);
    edited.setItemList(
        List.of(new Item(0, "Плов", Category.SECOND), new Item(1, "Компот", Category.BEVERAGE)));
    repository.save(edited);
    long plovIdAfterEdit = findItemId(City.KARAGANDA, DATE_12, "Плов");

    assertEquals(
        plovIdBeforeEdit,
        plovIdAfterEdit,
        "item_id блюда, которое не изменилось по имени между двумя save() в тот же день, "
            + "должен оставаться прежним - иначе будущий FK из order_items сломается "
            + "при повторном редактировании меню в течение дня");
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
  void getAllByDate_loadsOwnItemsInDisplayOrder_forEachMenu() {
    Menu almaty = buildMenu(City.ALMATA, DATE_13);
    almaty.setItemList(
        List.of(new Item(0, "Борщ", Category.FIRST), new Item(1, "Хлеб", Category.BREAD)));
    Menu astana = buildMenu(City.ASTANA, DATE_13);
    astana.setItemList(List.of(new Item(0, "Салат", Category.SALAD)));
    repository.save(almaty);
    repository.save(astana);

    Map<City, Menu> menusByCity =
        repository.getAll(DATE_13).stream()
            .collect(Collectors.toMap(Menu::getCity, Function.identity()));

    assertItemFieldsMatchInOrder(almaty.getItemList(), menusByCity.get(City.ALMATA).getItemList());
    assertItemFieldsMatchInOrder(astana.getItemList(), menusByCity.get(City.ASTANA).getItemList());
  }

  @Test
  void getById_returnsEmptyItemList_whenMenuHasNoItems() {
    Menu menu = buildMenu(City.KARAGANDA, DATE_11);
    menu.setItemList(List.of());
    repository.save(menu);

    Menu found = repository.getById(City.KARAGANDA.toString(), DATE_11);

    assertTrue(found.getItemList().isEmpty());
  }

  @Test
  void deleteBefore_removesMenusOfEveryCityDatedBeforeCutoff() {
    repository.save(buildMenu(City.ALMATA, BEFORE_DELETION_CUTOFF));
    repository.save(buildMenu(City.ASTANA, BEFORE_DELETION_CUTOFF.minusDays(1)));

    repository.deleteBefore(DELETION_CUTOFF);

    assertNull(repository.getById(City.ALMATA.toString(), BEFORE_DELETION_CUTOFF));
    assertNull(repository.getById(City.ASTANA.toString(), BEFORE_DELETION_CUTOFF.minusDays(1)));
  }

  @Test
  void deleteBefore_keepsMenusDatedOnOrAfterCutoff() {
    Menu onCutoff = buildMenu(City.KARAGANDA, DELETION_CUTOFF);
    Menu afterCutoff = buildMenu(City.KARAGANDA, DELETION_CUTOFF.plusDays(1));
    repository.save(onCutoff);
    repository.save(afterCutoff);

    repository.deleteBefore(DELETION_CUTOFF);

    assertEquals(onCutoff, repository.getById(City.KARAGANDA.toString(), DELETION_CUTOFF));
    assertEquals(
        afterCutoff, repository.getById(City.KARAGANDA.toString(), DELETION_CUTOFF.plusDays(1)));
  }

  @Test
  void deleteBefore_removesItemsOfDeletedMenu() throws SQLException {
    LocalDate date = BEFORE_DELETION_CUTOFF.minusDays(2);
    repository.save(buildMenu(City.ASTANA, date));
    long itemId = findItemId(City.ASTANA, date, "Плов");

    repository.deleteBefore(DELETION_CUTOFF);

    assertEquals(0, countItemsById(itemId));
  }

  private static int countItemsById(long itemId) throws SQLException {
    String sql = "SELECT COUNT(*) AS items FROM menu_items WHERE item_id = ?";
    try (Connection connection = PersistenceConfig.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, itemId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt("items");
      }
    }
  }

  private static long findItemId(City city, LocalDate date, String name) throws SQLException {
    String sql =
        "SELECT mi.item_id FROM menu_items mi "
            + "JOIN menus m ON m.id = mi.menu_id "
            + "WHERE m.city = ? AND m.date = ? AND mi.name = ?";
    try (Connection connection = PersistenceConfig.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, city.toString());
      statement.setObject(2, date);
      statement.setString(3, name);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong("item_id");
      }
    }
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
