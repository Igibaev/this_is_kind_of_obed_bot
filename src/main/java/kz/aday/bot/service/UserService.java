package kz.aday.bot.service;

import kz.aday.bot.model.User;
import kz.aday.bot.repository.BaseRepository;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class UserService extends BaseService<User> {

    public UserService() {
        super(new BaseRepository<>(new ConcurrentHashMap<>(), User.class, "user"));
    }
}
