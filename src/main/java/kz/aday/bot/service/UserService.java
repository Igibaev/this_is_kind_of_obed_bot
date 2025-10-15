/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.UserRepository;

public class UserService extends BaseService<User> {

  public UserService() {
    super(new UserRepository(new ConcurrentHashMap<>(), "user"));
  }
}
