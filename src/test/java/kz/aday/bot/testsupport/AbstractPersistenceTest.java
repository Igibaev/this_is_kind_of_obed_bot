/* (C) 2024 Igibaev */
package kz.aday.bot.testsupport;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import kz.aday.bot.configuration.BotConfig;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractPersistenceTest {

  private static final String PRODUCTION_STORE_PATH = "/app/data";

  @BeforeAll
  static void guardAgainstProductionStorePath() {
    assertNotEquals(
        PRODUCTION_STORE_PATH,
        BotConfig.getBotStorePath(),
        "Persistence tests resolved BOT_STORE_PATH to the production data folder. "
            + "Run these tests via Gradle (./gradlew test) so BOT_STORE_PATH points at a "
            + "disposable test directory instead of local.properties' production value.");
  }
}
