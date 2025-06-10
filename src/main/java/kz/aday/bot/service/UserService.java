package kz.aday.bot.service;

import kz.aday.bot.model.User;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.Repository;

import java.util.HashMap;


public class UserService {
    private final Repository<User> repository;

    public UserService() {
        this.repository = new BaseRepository<>(new HashMap<>(), User.class, "user");
    }

    public User getUser(Long chatId) {
        return repository.getById(chatId.toString());
    }

    public boolean isUserExist(Long chatId) {
        return repository.existById(chatId.toString());
    }

    public void saveUser(User user) {
        repository.save(user);
    }

    //    public boolean hasUserState(Long chatId) throws TelegramMessageException {
//        User user = getUser(chatId);
//        return user.getUserState() != null && user.getUserState() != UserState.NONE;
//    }
//
//    public void setUserState(Long chatId, UserState userState) throws TelegramMessageException {
//        User user = getUser(chatId);
//        user.setUserState(userState);
//        repository.save(user);
//    }
//
//    public void clearUserState(Long chatId) throws TelegramMessageException {
//        setUserState(chatId, UserState.NONE);
//    }
//
//    public Collection<User> getAllUsersByCity(City city) {
//        return repository.getAll().stream()
//                .filter(user -> user.getCity() == city)
//                .toList();
//    }
//
//    public Collection<User> getAllUsers() {
//        return repository.getAll();
//    }
}
