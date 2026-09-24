/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.repository.schema.Columns;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractRepositoryTest {

  private static final String EXISTING_ID = "existing";
  private static final String MISSING_ID = "missing";
  private static final String ENTITY = "entity";
  private static final LocalDate DATE = LocalDate.of(2031, 1, 1);
  private static final String COLUMN = "column";
  private static final long CHAT_ID = 42L;
  private static final int ITEM_ID = 7;
  private static final int ROW_NUM = 1;
  private static final String ITEM_NAME = "Плов";
  private static final String SQL = "SQL";

  private DataSource dataSource;
  private ResultSet resultSet;
  private TestRepository repository;

  @BeforeEach
  void setUp() {
    dataSource = mock(DataSource.class);
    resultSet = mock(ResultSet.class);
    repository = new TestRepository(dataSource);
  }

  @Test
  void existById_returnsTrue_whenGetByIdFindsEntity() {
    assertTrue(repository.existById(EXISTING_ID, DATE));
  }

  @Test
  void existById_returnsFalse_whenGetByIdReturnsNull() {
    assertFalse(repository.existById(MISSING_ID, DATE));
  }

  @Test
  void enumName_returnsName_whenValuePresent() {
    assertEquals(City.ALMATA.name(), AbstractRepository.enumName(City.ALMATA));
  }

  @Test
  void enumName_returnsNull_whenValueAbsent() {
    assertNull(AbstractRepository.enumName(null));
  }

  @Test
  void toChatId_parsesLong_whenChatIdPresent() {
    assertEquals(CHAT_ID, AbstractRepository.toChatId(String.valueOf(CHAT_ID)));
  }

  @Test
  void toChatId_returnsNull_whenChatIdAbsent() {
    assertNull(AbstractRepository.toChatId(null));
  }

  @Test
  void enumValue_parsesEnum_whenColumnHasValue() throws SQLException {
    when(resultSet.getString(COLUMN)).thenReturn(City.ASTANA.name());

    assertEquals(City.ASTANA, AbstractRepository.enumValue(resultSet, COLUMN, City.class));
  }

  @Test
  void enumValue_returnsNull_whenColumnIsNull() throws SQLException {
    when(resultSet.getString(COLUMN)).thenReturn(null);

    assertNull(AbstractRepository.enumValue(resultSet, COLUMN, City.class));
  }

  @Test
  void chatIdValue_returnsStringChatId_whenColumnIsNotNull() throws SQLException {
    when(resultSet.getObject(COLUMN, Long.class)).thenReturn(CHAT_ID);

    assertEquals(String.valueOf(CHAT_ID), AbstractRepository.chatIdValue(resultSet, COLUMN));
  }

  @Test
  void chatIdValue_returnsNull_whenColumnIsNull() throws SQLException {
    when(resultSet.getObject(COLUMN, Long.class)).thenReturn(null);

    assertNull(AbstractRepository.chatIdValue(resultSet, COLUMN));
  }

  @Test
  void itemMapper_readsIdNameAndCategory() throws SQLException {
    when(resultSet.getObject(Columns.ITEM_ID, Integer.class)).thenReturn(ITEM_ID);
    when(resultSet.getString(Columns.NAME)).thenReturn(ITEM_NAME);
    when(resultSet.getString(Columns.CATEGORY)).thenReturn(Category.SECOND.name());

    Item item = AbstractRepository.ITEM_MAPPER.mapRow(resultSet, ROW_NUM);

    assertEquals(ITEM_ID, item.getId());
    assertEquals(ITEM_NAME, item.getName());
    assertEquals(Category.SECOND, item.getCategory());
  }

  @Test
  void itemMapper_keepsNullIdAndCategory_whenColumnsAreNull() throws SQLException {
    when(resultSet.getString(Columns.NAME)).thenReturn(ITEM_NAME);

    Item item = AbstractRepository.ITEM_MAPPER.mapRow(resultSet, ROW_NUM);

    assertNull(item.getId());
    assertNull(item.getCategory());
  }

  @Test
  void queryGroupedByOwnerId_returnsEmptyMap_withoutQuerying_whenNoOwnerIds() {
    Map<Long, List<Item>> grouped =
        repository.queryGroupedByOwnerId(SQL, List.of(), AbstractRepository.ITEM_MAPPER);

    assertTrue(grouped.isEmpty());
    verifyNoInteractions(dataSource);
  }

  private static class TestRepository extends AbstractRepository<String> {
    private static final Map<String, String> ENTITIES = Map.of(EXISTING_ID, ENTITY);

    TestRepository(DataSource dataSource) {
      super(dataSource);
    }

    @Override
    public String getById(String id, LocalDate date) {
      return ENTITIES.get(id);
    }

    @Override
    public Collection<String> getAll(LocalDate date) {
      return List.of();
    }

    @Override
    public Collection<String> getAll() {
      return List.of();
    }

    @Override
    public void save(String entity) {}

    @Override
    public void deleteById(String id, LocalDate date) {}
  }
}
