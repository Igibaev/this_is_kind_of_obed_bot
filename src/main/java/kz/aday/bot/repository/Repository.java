/* (C) 2024 Igibaev */
package kz.aday.bot.repository;

import java.util.Collection;

public interface Repository<T> {
  T getById(String id);

  boolean existById(String id);

  Collection<T> getAll();

  void save(T t);

  void clearAll();

  void deleteById(String id);

  void clearStorage();
}
