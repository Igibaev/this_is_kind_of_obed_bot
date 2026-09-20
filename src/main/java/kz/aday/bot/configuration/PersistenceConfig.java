/* (C) 2024 Igibaev */
package kz.aday.bot.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

@Slf4j
public class PersistenceConfig {

  private static final String MIGRATIONS_LOCATION = "classpath:db/migration";

  private PersistenceConfig() {}

  public static DataSource getDataSource() {
    return Holder.DATA_SOURCE;
  }

  private static DataSource buildDataSource() {
    migrateSchema();

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(BotConfig.getDatabaseUrl());
    hikariConfig.setUsername(BotConfig.getDatabaseUsername());
    hikariConfig.setPassword(BotConfig.getDatabasePassword());
    log.info("Creating HikariCP DataSource for [{}]", BotConfig.getDatabaseUrl());
    return new HikariDataSource(hikariConfig);
  }

  private static void migrateSchema() {
    log.info("Running Flyway migrations from [{}]", MIGRATIONS_LOCATION);
    Flyway.configure()
        .dataSource(
            BotConfig.getDatabaseUrl(),
            BotConfig.getDatabaseUsername(),
            BotConfig.getDatabasePassword())
        .locations(MIGRATIONS_LOCATION)
        .load()
        .migrate();
  }

  private static final class Holder {
    private static final DataSource DATA_SOURCE = buildDataSource();
  }
}
