/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.time.LocalDate;
import java.util.Collection;

public interface Repository<T> {
  T getById(String id);

  boolean existById(String id);

  Collection<T> getAll(LocalDate date);

  void save(T t);

  void clearLastWeek();

  void deleteById(String id);

  void clearStorage();
}
