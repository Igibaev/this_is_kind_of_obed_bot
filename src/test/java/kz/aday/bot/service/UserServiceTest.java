/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  private static final String USER_ID = "42";

  private Repository<User> repository;
  private UserService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    repository = mock(Repository.class);
    service = new UserService(repository);
  }

  @Test
  void findById_returnsUserFromRepository() {
    User user = user();
    when(repository.getById(eq(USER_ID), any(LocalDate.class))).thenReturn(user);

    assertSame(user, service.findById(USER_ID));
  }

  @Test
  void findByIdOptional_returnsUser_whenRepositoryHasIt() {
    User user = user();
    when(repository.getById(eq(USER_ID), any(LocalDate.class))).thenReturn(user);

    assertEquals(Optional.of(user), service.findByIdOptional(USER_ID));
  }

  @Test
  void findByIdOptional_returnsEmpty_whenRepositoryHasNoUser() {
    when(repository.getById(eq(USER_ID), any(LocalDate.class))).thenReturn(null);

    assertTrue(service.findByIdOptional(USER_ID).isEmpty());
  }

  @Test
  void existsById_delegatesToRepository() {
    when(repository.existById(eq(USER_ID), any(LocalDate.class))).thenReturn(true);

    assertTrue(service.existsById(USER_ID));
    assertFalse(service.existsById("other"));
  }

  @Test
  void findAll_returnsAllUsersFromRepository() {
    User user = user();
    when(repository.getAll(any(LocalDate.class))).thenReturn(List.of(user));

    assertEquals(List.of(user), List.copyOf(service.findAll()));
  }

  @Test
  void save_persistsUserAndReturnsIt() {
    User user = user();

    assertSame(user, service.save(user));
    verify(repository).save(user);
  }

  private static User user() {
    return User.builder().chatId(Long.parseLong(USER_ID)).build();
  }
}
