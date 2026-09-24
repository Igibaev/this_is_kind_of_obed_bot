/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.repository.schema.Columns;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowCallbackHandler;

public class MenuRepository extends AbstractRepository<Menu> {

  private static final String SELECT_ALL_MENUS =
      "SELECT id, city, date, status, deadline, available, notificated, message FROM menus";
  private static final String SELECT_MENU_BY_CITY_AND_DATE =
      SELECT_ALL_MENUS + " WHERE city = ? AND date = ?";
  private static final String SELECT_MENUS_BY_DATE = SELECT_ALL_MENUS + " WHERE date = ?";
  private static final String SELECT_ITEMS_BY_MENU_IDS =
      "SELECT menu_id AS owner_id, item_id, name, category FROM menu_items "
          + "WHERE menu_id IN (:"
          + OWNER_IDS_PARAMETER
          + ") ORDER BY display_order ASC";
  private static final String UPSERT_MENU =
      "INSERT INTO menus (city, date, status, deadline, available, notificated, message) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?) "
          + "ON CONFLICT (city, date) DO UPDATE SET "
          + "status = EXCLUDED.status, "
          + "deadline = EXCLUDED.deadline, "
          + "available = EXCLUDED.available, "
          + "notificated = EXCLUDED.notificated, "
          + "message = EXCLUDED.message "
          + "RETURNING id";
  private static final String SELECT_EXISTING_ITEM_IDS_BY_MENU_ID =
      "SELECT item_id, name FROM menu_items WHERE menu_id = ?";
  private static final String DELETE_ITEM_BY_ID = "DELETE FROM menu_items WHERE item_id = ?";
  private static final String SHIFT_DISPLAY_ORDER_TO_TEMP =
      "UPDATE menu_items SET display_order = -item_id WHERE item_id = ?";
  private static final String UPDATE_ITEM =
      "UPDATE menu_items SET display_order = ?, category = ? WHERE item_id = ?";
  private static final String INSERT_ITEM =
      "INSERT INTO menu_items (menu_id, display_order, name, category) "
          + "VALUES (?, ?, ?, ?) RETURNING item_id";
  private static final String DELETE_MENU_BY_CITY_AND_DATE =
      "DELETE FROM menus WHERE city = ? AND date = ?";
  private static final String DELETE_MENUS_BEFORE = "DELETE FROM menus WHERE date < ?";

  public MenuRepository(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  public Menu getById(String id, LocalDate date) {
    return DataAccessUtils.singleResult(loadMenus(SELECT_MENU_BY_CITY_AND_DATE, id, date));
  }

  @Override
  public Collection<Menu> getAll(LocalDate date) {
    return loadMenus(SELECT_MENUS_BY_DATE, date);
  }

  @Override
  public Collection<Menu> getAll() {
    return loadMenus(SELECT_ALL_MENUS);
  }

  @Override
  public void save(Menu menu) {
    LocalDate date = menu.getStorageDate();
    transactionTemplate.executeWithoutResult(
        status -> replaceItems(upsertMenu(menu, date), menu.getItemList()));
    log.info("Saved menu [{}] for date [{}]", menu.getId(), date);
  }

  @Override
  public void deleteById(String id, LocalDate date) {
    int deleted = jdbcTemplate.update(DELETE_MENU_BY_CITY_AND_DATE, id, date);
    log.info("Deleted [{}] menu(s) with id [{}] for date [{}]", deleted, id, date);
  }

  public void deleteBefore(LocalDate cutoff) {
    int deleted = jdbcTemplate.update(DELETE_MENUS_BEFORE, cutoff);
    log.info("Deleted [{}] menu(s) before [{}]", deleted, cutoff);
  }

  private List<Menu> loadMenus(String sql, Object... args) {
    Map<Long, Menu> menusById = queryIndexedById(sql, MenuRepository::mapHeaderRow, args);
    Map<Long, List<Item>> itemsByMenuId =
        queryGroupedByOwnerId(SELECT_ITEMS_BY_MENU_IDS, menusById.keySet(), ITEM_MAPPER);
    menusById.forEach(
        (menuId, menu) -> menu.setItemList(itemsByMenuId.getOrDefault(menuId, new ArrayList<>())));
    return new ArrayList<>(menusById.values());
  }

  private static Menu mapHeaderRow(ResultSet resultSet, int rowNum) throws SQLException {
    Menu menu = new Menu();
    menu.setCity(enumValue(resultSet, Columns.CITY, City.class));
    menu.setDate(resultSet.getObject(Columns.DATE, LocalDate.class).toString());
    menu.setStatus(enumValue(resultSet, Columns.STATUS, Status.class));
    menu.setDeadline(resultSet.getObject(Columns.DEADLINE, LocalDateTime.class));
    menu.setAvailable(resultSet.getObject(Columns.AVAILABLE, Boolean.class));
    menu.setNotificated(resultSet.getObject(Columns.NOTIFICATED, Boolean.class));
    menu.setMessage(resultSet.getString(Columns.MESSAGE));
    return menu;
  }

  private long upsertMenu(Menu menu, LocalDate date) {
    return jdbcTemplate.queryForObject(
        UPSERT_MENU,
        Long.class,
        enumName(menu.getCity()),
        date,
        enumName(menu.getStatus()),
        menu.getDeadline(),
        menu.getAvailable(),
        menu.getNotificated(),
        menu.getMessage());
  }

  private void replaceItems(long menuId, List<Item> items) {
    Map<String, Long> existingIdByName = new HashMap<>();
    jdbcTemplate.query(
        SELECT_EXISTING_ITEM_IDS_BY_MENU_ID,
        (RowCallbackHandler)
            resultSet ->
                existingIdByName.put(
                    resultSet.getString(Columns.NAME), resultSet.getLong(Columns.ITEM_ID)),
        menuId);

    Set<String> newNames = items.stream().map(Item::getName).collect(Collectors.toSet());
    jdbcTemplate.batchUpdate(
        DELETE_ITEM_BY_ID,
        existingIdByName.entrySet().stream()
            .filter(existing -> !newNames.contains(existing.getKey()))
            .map(existing -> new Object[] {existing.getValue()})
            .toList());

    jdbcTemplate.batchUpdate(
        SHIFT_DISPLAY_ORDER_TO_TEMP,
        items.stream()
            .map(item -> existingIdByName.get(item.getName()))
            .filter(Objects::nonNull)
            .map(existingId -> new Object[] {existingId})
            .toList());

    List<Object[]> updates = new ArrayList<>();
    for (int displayOrder = 0; displayOrder < items.size(); displayOrder++) {
      Item item = items.get(displayOrder);
      Long existingId = existingIdByName.get(item.getName());
      if (existingId != null) {
        item.setId(existingId.intValue());
        updates.add(new Object[] {displayOrder, enumName(item.getCategory()), existingId});
      } else {
        item.setId(
            jdbcTemplate.queryForObject(
                INSERT_ITEM,
                Integer.class,
                menuId,
                displayOrder,
                item.getName(),
                enumName(item.getCategory())));
      }
    }
    jdbcTemplate.batchUpdate(UPDATE_ITEM, updates);
  }
}
