package kz.aday.bot.model;

import kz.aday.bot.exception.TelegramMessageException;
import kz.aday.bot.messages.Messages;
import lombok.Data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Data
public class Report implements Id {
    private City city;
    private Collection<Order> orderList;
    private Map<Item,Integer> itemsCount = new HashMap<>();

    public Report(City city, Collection<Order> orderList) {
        this.city = city;
        this.orderList = orderList;
    }

    @Override
    public String getId() {
        return city.toString();
    }
}
