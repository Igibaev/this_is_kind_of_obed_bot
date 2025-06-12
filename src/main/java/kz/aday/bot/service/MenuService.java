package kz.aday.bot.service;


import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.repository.BaseRepository;
import kz.aday.bot.repository.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MenuService extends BaseService<Menu>{
    public MenuService() {
        super(new BaseRepository<>(new ConcurrentHashMap<>(), Menu.class, "menu"));
        Menu menu = new Menu();
        menu.setCity(City.ASTANA);
    }
}
