package kz.aday.bot.repository;

import java.util.Collection;

public interface Repository<T> {
    T getById(String id);

    Collection<T> getAll();

    void save(T t);

    void clearAll();
}
