/* (C) 2024 Igibaev */
package kz.aday.bot.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import kz.aday.bot.configuration.BotConfig;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractDbPersistenceTest {

  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withImagePullPolicy(PullPolicy.alwaysPull());

  static {
    POSTGRES.start();
    System.setProperty("DB_URL", POSTGRES.getJdbcUrl());
    System.setProperty("DB_USERNAME", POSTGRES.getUsername());
    System.setProperty("DB_PASSWORD", POSTGRES.getPassword());
  }

  @BeforeAll
  static void guardAgainstProductionDatabase() {
    assertEquals(
        POSTGRES.getJdbcUrl(),
        BotConfig.getDatabaseUrl(),
        "Persistence tests resolved DB_URL to something other than the disposable Testcontainers "
            + "Postgres instance. A real DB_URL environment variable is likely set outside the "
            + "test process.");
  }
}
