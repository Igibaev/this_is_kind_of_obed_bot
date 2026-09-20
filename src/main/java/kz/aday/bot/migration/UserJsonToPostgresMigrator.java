/* (C) 2024 Igibaev */
package kz.aday.bot.migration;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.JdbcUserRepository;
import kz.aday.bot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserJsonToPostgresMigrator {

  private static final String USER_STORAGE_PATH = "user";

  private UserJsonToPostgresMigrator() {}

  public static void main(String[] args) {
    UserRepository jsonRepository =
        new UserRepository(new ConcurrentHashMap<>(), USER_STORAGE_PATH);
    JdbcUserRepository postgresRepository =
        new JdbcUserRepository(PersistenceConfig.getDataSource());

    Collection<User> users = jsonRepository.getAll();
    log.info("Migrating [{}] user(s) from JSON storage to Postgres", users.size());

    for (User user : users) {
      postgresRepository.save(user);
    }

    log.info("Migration complete");
  }
}
