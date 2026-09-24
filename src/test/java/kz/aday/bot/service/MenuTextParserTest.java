/* (C) 2024 Igibaev */
package kz.aday.bot.service;

import static kz.aday.bot.testsupport.TestFixtures.menuTextWithDeadline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import kz.aday.bot.exception.TelegramMessageException;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.Test;

class MenuTextParserTest {

  @Test
  void parseMenu_assignsItemsToCategories_whenEveryCategoryAppearsOnce() throws Exception {
    String text =
        menuTextWithDeadline(
            "Первое:\nСуп\nВторое:\nПлов\nСалаты:\nЦезарь\nВыпечка:\nБулочка\nХлеб");

    Menu menu = MenuTextParser.parseMenu(text);

    List<Item> items = menu.getItemList();
    assertEquals(
        List.of("Суп", "Плов", "Цезарь", "Булочка", Category.BREAD.getValue()),
        items.stream().map(Item::getName).toList());
    assertEquals(
        List.of(Category.FIRST, Category.SECOND, Category.SALAD, Category.BAKERY, Category.BREAD),
        items.stream().map(Item::getCategory).toList());
  }

  @Test
  void parseMenu_throwsDuplicateCategory_whenCategoryHeaderRepeated() {
    String text =
        menuTextWithDeadline("Первое:\nСуп\nВторое:\nПлов\nСалаты:\nЦезарь\nВторое:\nБулочка");

    assertDuplicateCategory(text, Category.SECOND);
  }

  @Test
  void parseMenu_throwsDuplicateCategory_whenCategoryRepeatedWithDifferentSpelling() {
    String text = menuTextWithDeadline("Салат:\nЦезарь\nСАЛАТЫ:\nОливье");

    assertDuplicateCategory(text, Category.SALAD);
  }

  @Test
  void parseMenu_throwsDuplicateCategory_whenBreadRepeated() {
    String text = menuTextWithDeadline("Второе:\nПлов\nХлеб\nХлеб");

    assertDuplicateCategory(text, Category.BREAD);
  }

  private static void assertDuplicateCategory(String text, Category duplicatedCategory) {
    TelegramMessageException exception =
        assertThrows(TelegramMessageException.class, () -> MenuTextParser.parseMenu(text));
    assertEquals(
        Messages.MENU_CATEGORY_DUPLICATED.getText(duplicatedCategory.getValue()),
        exception.getMessage());
  }
}
