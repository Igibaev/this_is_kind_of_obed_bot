/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.Item;
import kz.aday.bot.repository.schema.Columns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class AbstractRepository<T> implements Repository<T> {
  protected static final String OWNER_IDS_PARAMETER = "ownerIds";

  protected static final RowMapper<Item> ITEM_MAPPER =
      (resultSet, rowNum) ->
          new Item(
              resultSet.getObject(Columns.ITEM_ID, Integer.class),
              resultSet.getString(Columns.NAME),
              enumValue(resultSet, Columns.CATEGORY, Category.class));

  protected final Logger log = LoggerFactory.getLogger(getClass());
  protected final JdbcTemplate jdbcTemplate;
  protected final NamedParameterJdbcTemplate namedJdbcTemplate;
  protected final TransactionTemplate transactionTemplate;

  protected AbstractRepository(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    this.transactionTemplate =
        new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public boolean existById(String id, LocalDate date) {
    return getById(id, date) != null;
  }

  protected <R> Map<Long, R> queryIndexedById(String sql, RowMapper<R> mapper, Object... args) {
    Map<Long, R> indexed = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        (RowCallbackHandler)
            resultSet ->
                indexed.put(
                    resultSet.getLong(Columns.ID), mapper.mapRow(resultSet, resultSet.getRow())),
        args);
    return indexed;
  }

  protected <R> Map<Long, List<R>> queryGroupedByOwnerId(
      String sql, Collection<Long> ownerIds, RowMapper<R> mapper) {
    Map<Long, List<R>> grouped = new LinkedHashMap<>();
    if (ownerIds.isEmpty()) {
      return grouped;
    }
    namedJdbcTemplate.query(
        sql,
        Map.of(OWNER_IDS_PARAMETER, ownerIds),
        (RowCallbackHandler)
            resultSet ->
                grouped
                    .computeIfAbsent(resultSet.getLong(Columns.OWNER_ID), key -> new ArrayList<>())
                    .add(mapper.mapRow(resultSet, resultSet.getRow())));
    return grouped;
  }

  protected static String enumName(Enum<?> value) {
    return value != null ? value.name() : null;
  }

  protected static Long toChatId(String chatId) {
    return chatId != null ? Long.valueOf(chatId) : null;
  }

  protected static <E extends Enum<E>> E enumValue(
      ResultSet resultSet, String column, Class<E> type) throws SQLException {
    String value = resultSet.getString(column);
    return value != null ? Enum.valueOf(type, value) : null;
  }

  protected static String chatIdValue(ResultSet resultSet, String column) throws SQLException {
    Long value = resultSet.getObject(column, Long.class);
    return value != null ? String.valueOf(value) : null;
  }
}
