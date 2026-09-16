/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.time.LocalDate;
import java.util.Collection;

public interface Repository<T> {
  T getById(String id, LocalDate date);

  boolean existById(String id, LocalDate date);

  Collection<T> getAll(LocalDate date);

  Collection<T> getAll();

  void save(T t);

  void clearLastWeek();

  void deleteById(String id, LocalDate date);

  void clearStorage();
}
