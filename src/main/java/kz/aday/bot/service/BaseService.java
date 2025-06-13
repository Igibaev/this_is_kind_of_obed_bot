package kz.aday.bot.service;

import java.util.Collection;
import java.util.Optional;
import kz.aday.bot.model.Id;
import kz.aday.bot.repository.Repository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseService<T extends Id> {
  protected final Repository<T> repository;

  public BaseService(Repository<T> repository) {
    this.repository = repository;
  }

  public T findById(String id) {
    log.debug("Attempting to find entity by ID: {}", id);
    T entity = repository.getById(id);
    if (entity == null) {
      log.warn("Entity with ID {} not found.", id);
    } else {
      log.debug("Found entity with ID: {}", id);
    }
    return entity;
  }

  public Optional<T> findByIdOptional(String id) {
    log.debug("Attempting to find entity (Optional) by ID: {}", id);
    Optional<T> entityOptional = Optional.ofNullable(repository.getById(id));
    if (entityOptional.isEmpty()) {
      log.warn("Entity with ID {} not found (Optional).", id);
    } else {
      log.debug("Found entity (Optional) with ID: {}", id);
    }
    return entityOptional;
  }

  public boolean existsById(String id) {
    log.debug("Checking existence of entity with ID: {}", id);
    boolean exists = repository.existById(id);
    log.debug("Entity with ID {} exists: {}", id, exists);
    return exists;
  }

  public Collection<T> findAll() {
    log.debug("Retrieving all entities.");
    Collection<T> entities = repository.getAll();
    log.debug("Retrieved {} entities.", entities.size());
    return entities;
  }

  public T save(T entity) {
    String entityId = entity.getId();
    boolean isNew =
        !repository.existById(entityId); // Проверяем, новая ли это сущность или обновление
    repository.save(entity);
    if (isNew) {
      log.info("Saved new entity with ID: {}", entityId);
    } else {
      log.info("Updated existing entity with ID: {}", entityId);
    }
    return entity;
  }

  public void deleteAll() {
    log.warn(
        "Clearing all entities from the repository."); // WARN, так как это деструктивная операция
    repository.clearAll();
    log.info("All entities cleared.");
  }
}
