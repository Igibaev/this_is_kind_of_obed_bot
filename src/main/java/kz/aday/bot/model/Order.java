package kz.aday.bot.model;

import kz.aday.bot.exception.TelegramMessageException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order implements Id {
    private Long chatId;
    private String username;
    private Status status;
    private Set<Item> orderItemList = new HashSet<>(5);
    private Set<Category> categoryItemList = new HashSet<>(5);

    @Override
    public String getId() {
        return chatId.toString();
    }

    private void addItem(Item item) {
        categoryItemList.add(item.getCategory());
        orderItemList.add(item);
    }

    public void removeItem(Item item) {
        categoryItemList.remove(item.getCategory());
        orderItemList.remove(item);
    }

    @Override
    public String toString() {
        return username+": " + orderItemList;
    }
}
