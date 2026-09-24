/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.Repository;
import kz.aday.bot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserService {
  private final Repository<User> repository;

  public UserService() {
    this(new UserRepository(PersistenceConfig.getDataSource()));
  }

  UserService(Repository<User> repository) {
    this.repository = repository;
  }

  public User findById(String id) {
    return repository.getById(id, LocalDate.now());
  }

  public Optional<User> findByIdOptional(String id) {
    return Optional.ofNullable(findById(id));
  }

  public boolean existsById(String id) {
    return repository.existById(id, LocalDate.now());
  }

  public Collection<User> findAll() {
    return repository.getAll(LocalDate.now());
  }

  public User save(User user) {
    repository.save(user);
    log.info("Saved user with ID: {}", user.getId());
    return user;
  }
}
