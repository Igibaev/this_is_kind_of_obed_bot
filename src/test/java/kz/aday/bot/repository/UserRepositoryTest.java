/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import org.junit.jupiter.api.Test;

class UserRepositoryTest extends AbstractDbPersistenceTest {

  private final UserRepository repository = new UserRepository(PersistenceConfig.getDataSource());

  @Test
  void existById_returnsFalse_whenUserWasNeverSaved() {
    assertFalse(repository.existById("920000001", null));
  }

  @Test
  void getById_returnsNull_whenUserWasNeverSaved() {
    assertNull(repository.getById("920000002", null));
  }

  @Test
  void getAll_returnsEveryPreviouslySavedUser() {
    User first = buildUser(920000003L);
    User second = buildUser(920000004L);

    repository.save(first);
    repository.save(second);

    Collection<User> all = repository.getAll();

    assertTrue(all.contains(first));
    assertTrue(all.contains(second));
  }

  @Test
  void save_updatesExistingRow_insteadOfDuplicatingIt_whenChatIdAlreadyExists() {
    User original = buildUser(920000005L);
    repository.save(original);

    User updated = buildUser(920000005L);
    updated.setPreferedName("Updated Name");
    updated.setCity(City.KARAGANDA);
    repository.save(updated);

    User found = repository.getById(original.getId(), null);
    assertEquals("Updated Name", found.getPreferedName());
    assertEquals(City.KARAGANDA, found.getCity());
    assertEquals(
        1, repository.getAll().stream().filter(u -> u.getId().equals(original.getId())).count());
  }

  private static User buildUser(Long chatId) {
    return User.builder()
        .chatId(chatId)
        .preferedName("Repo Test")
        .lastMessageId(7)
        .city(City.ALMATA)
        .role(User.Role.USER)
        .state(State.NONE)
        .status(Status.READY)
        .build();
  }
}
