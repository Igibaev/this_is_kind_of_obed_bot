/* (C) 2024 Igibaev */
package kz.aday.bot.messages;

public interface Messages {
  String RETURN_TO_MENU = "Чтобы вернуться в меню нажмите /cancel";
  String DEADLINE_IS_NEAR_MAKE_AN_ORDER = "Скоро дедлайн, успей заказать еду.";
  String MENU_IS_CLOSED = "Меню для заказов закрыто";

  String WILL_COME_DAY_QUESTION = "За какой день хочешь отметиться?";
  String WILL_COME_TODAY_QUESTION = "Пойдешь ли ты сегодня в офис?";
  String WILL_COME_TOMORROW_QUESTION = "Пойдешь ли ты завтра в офис?";
  String THANKS_WILL_COME_TODAY = "Отлично! Записал: сегодня ты будешь в офисе.";
  String THANKS_WILL_COME_TOMORROW = "Отлично! Записал: завтра ты будешь в офисе.";
  String THANKS_WONT_COME_TODAY = "Понял, сегодня тебя в офисе не будет.";
  String THANKS_WONT_COME_TOMORROW = "Понял, завтра тебя в офисе не будет.";

  String MENU_TEMPLATE =
      "Шаблон меню:\n"
          + "Возможны только 5 категории блюд(первое, второе, салат, выпечка, хлеб)\n"
          + "\n"
          + "Первое:\n"
          + "Блюда перечисляются через отступ строки\n"
          + "\n"
          + "Второе:\n"
          + "Блюдо 1\n"
          + "Блюдо 2\n"
          + "\n"
          + "Салат:\n"
          + "1. Блюдо('1. ' это затираться будет)\n"
          + "\n"
          + "Выпечка:\n"
          + "\n"
          + "Хлеб: (если хлеба нету,  либо он всегда к заказу идет, то лучше его убрать)\n"
          + "\n"
          + "Дедлайн 11:00. (дедлайн можно указывать так, можно просто время указывать в формате HH:mm)\n"
          + "\n"
          + "Чтобы отменить нажми /cancel\n";
}
