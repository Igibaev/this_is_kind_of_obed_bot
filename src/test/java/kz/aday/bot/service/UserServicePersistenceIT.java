/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.testsupport.AbstractPersistenceTest;
import org.junit.jupiter.api.Test;

class UserServicePersistenceIT extends AbstractPersistenceTest {

  @Test
  void save_thenFindById_returnsPersistedUser() {
    User user = buildUser(910000001L);
    UserService service = new UserService();

    service.save(user);
    User found = service.findById(user.getId());

    assertEquals(user, found);
  }

  @Test
  void newServiceInstance_seesUserSavedByPreviousInstance() {
    User user = buildUser(910000002L);
    new UserService().save(user);

    User found = new UserService().findById(user.getId());

    assertEquals(user, found);
  }

  @Test
  void deleteById_removesUserFromStorageForFutureInstances() {
    User user = buildUser(910000003L);
    UserService service = new UserService();
    service.save(user);
    assertTrue(service.existsById(user.getId()));

    service.deleteById(user.getId());

    assertFalse(service.existsById(user.getId()));
    assertNull(new UserService().findById(user.getId()));
  }

  private static User buildUser(Long chatId) {
    return User.builder()
        .chatId(chatId)
        .preferedName("Persist Test")
        .lastMessageId(5)
        .city(City.ALMATA)
        .role(User.Role.USER)
        .state(State.NONE)
        .status(Status.READY)
        .build();
  }
}
