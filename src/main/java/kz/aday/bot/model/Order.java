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

    public void addItemOrRemoveItem(Item item, Map<Category, Set<Category>> disjointCategoriesSet) throws TelegramMessageException {
        if (orderItemList.contains(item)) {
            removeItem(item);
            return;
        }
        if (categoryItemList.contains(item.getCategory())) {
            throw new TelegramMessageException("Допустимо выбирать по одному блюду из категории.");
        }

        Set<Category> disjointCategories = disjointCategoriesSet.get(item.getCategory());
        if (disjointCategories != null && categoryItemList.containsAll(disjointCategories)) {
            throw new TelegramMessageException("Ты уже выбрал, две категории, исключи одну из них чтобы выбрать "+item.getCategory());
        }
        addItem(item);
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
