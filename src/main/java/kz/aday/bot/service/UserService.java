/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import java.util.concurrent.ConcurrentHashMap;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.BaseRepository;

public class UserService extends BaseService<User> {

  public UserService() {
    super(new BaseRepository<>(new ConcurrentHashMap<>(), User.class, "user"));
  }
}
