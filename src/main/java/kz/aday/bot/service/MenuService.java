package kz.aday.bot.service;


import kz.aday.bot.model.Menu;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.Repository;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MenuService {
    private final Repository<Menu> repository;

    public MenuService() {
        this.repository = new BaseRepository<>(new HashMap<>(), Menu.class, "menu");
    }
//
//    public void saveMenuRules(MenuRules menuRules) {
//        menuRulesRepository.save(menuRules);
//    }
//
//    public void createMenuRules(City city, Map<Category, Set<Category>> categoryRules) {
//        MenuRules menuRules = new MenuRules(city, categoryRules);
//        menuRulesRepository.save(menuRules);
//    }
//
//    public MenuRules getMenuRules(City city) {
//        return menuRulesRepository.getById(city.toString()).orElse(null);
//
//    }
//
//    public Menu getMenu(City city) throws TelegramMessageException {
//        return repository.getById(city.toString()).orElseThrow(() -> new TelegramMessageException("Меню еще не создано."));
//    }
//
//    public void submitMenu(City city) throws TelegramMessageException {
//        Menu menu = getMenu(city);
//        menu.setStatus(Status.READY);
//        repository.save(menu);
//    }
//
//    public Menu createMenu(City city, String message) throws TelegramMessageException {
//        Menu menu = parseMenu(message);
//        menu.setCity(city);
//        repository.save(menu);
//        return menu;
//    }
//
//    public Menu changeMenu(City city, String message) throws TelegramMessageException {
//        if (repository.getById(city.toString()).isPresent()) {
//            Menu menu = parseMenu(message);
//            menu.setCity(city);
//            repository.save(menu);
//            return menu;
//        }
//        throw new TelegramMessageException("");
//    }
//
//    public void changeDeadline(City city, String message) throws TelegramMessageException {
//        if (repository.getById(city.toString()).isEmpty()) {
//            throw new TelegramMessageException("");
//        }
//        LocalDateTime deadline = menuParser.parseDeadline(message);
//        repository.getById(city.toString())
//                .ifPresent(menu -> {
//                    menu.setDeadline(deadline);
//                    repository.save(menu);
//                });
//    }
//
//    private Menu parseMenu(String message) throws TelegramMessageException {
//        return menuParser.parseMenu(message);
//    }
//
//    public static MenuRules defaultMenuRules(City city) {
//        Map<Category, Set<Category>> categoryMapRules = new EnumMap<>(Category.class);
//
//        categoryMapRules.put(Category.FIRST, Set.of(Category.SECOND, Category.BAKERY));
//        categoryMapRules.put(Category.SECOND, Set.of(Category.BAKERY, Category.FIRST));
//        categoryMapRules.put(Category.BAKERY, Set.of(Category.FIRST, Category.SECOND));
//
//        return new MenuRules(city, categoryMapRules);
//    }

}
