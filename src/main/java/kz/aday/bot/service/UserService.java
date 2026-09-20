/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.JdbcUserRepository;

public class UserService extends BaseService<User> {

  public UserService() {
    super(new JdbcUserRepository(PersistenceConfig.getDataSource()));
  }
}
