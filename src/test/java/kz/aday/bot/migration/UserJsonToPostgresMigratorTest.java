/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.JdbcUserRepository;
import kz.aday.bot.repository.UserRepository;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UserJsonToPostgresMigratorTest extends AbstractDbPersistenceTest {

  private final UserRepository jsonRepository =
      new UserRepository(new ConcurrentHashMap<>(), "user");
  private final JdbcUserRepository postgresRepository =
      new JdbcUserRepository(PersistenceConfig.getDataSource());

  @AfterEach
  void cleanUpMigratedUsers() {
    jsonRepository.deleteById("920000010", null);
    jsonRepository.deleteById("920000011", null);
    postgresRepository.deleteById("920000010", null);
    postgresRepository.deleteById("920000011", null);
  }

  @Test
  void main_migratesEveryUserFromJsonStorageIntoPostgres() {
    jsonRepository.save(buildUser(920000010L, "First"));
    jsonRepository.save(buildUser(920000011L, "Second"));

    UserJsonToPostgresMigrator.main(new String[0]);

    User migratedFirst = postgresRepository.getById("920000010", null);
    User migratedSecond = postgresRepository.getById("920000011", null);

    assertEquals("First", migratedFirst.getPreferedName());
    assertEquals(City.ALMATA, migratedFirst.getCity());
    assertEquals("Second", migratedSecond.getPreferedName());
    assertEquals(City.KARAGANDA, migratedSecond.getCity());
  }

  @Test
  void main_overwritesExistingRow_whenUserWasAlreadyMigratedBefore() {
    jsonRepository.save(buildUser(920000010L, "First"));
    postgresRepository.save(buildUser(920000010L, "Stale"));

    UserJsonToPostgresMigrator.main(new String[0]);

    User migrated = postgresRepository.getById("920000010", null);
    assertEquals("First", migrated.getPreferedName());
  }

  @Test
  void main_doesNothing_whenNoJsonUsersExist() {
    UserJsonToPostgresMigrator.main(new String[0]);

    assertNull(postgresRepository.getById("920000010", null));
  }

  private static User buildUser(Long chatId, String preferedName) {
    return User.builder()
        .chatId(chatId)
        .preferedName(preferedName)
        .lastMessageId(7)
        .city(chatId == 920000010L ? City.ALMATA : City.KARAGANDA)
        .role(User.Role.USER)
        .state(State.NONE)
        .status(Status.READY)
        .build();
  }
}
