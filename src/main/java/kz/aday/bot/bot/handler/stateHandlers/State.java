/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

public enum State {
  // --- Специальные/технические стейты ---
  NONE("", Type.INTERNAL), // Начальное или неопределенное состояние
  DEFAULT("", Type.INTERNAL), // Состояние по умолчанию, возможно, после какого-то действия

  // --- Стейты, требующие ввода данных ---
  CHOOSE_CITY("Выберите город", Type.INPUT),
  SET_USERNAME_THEN_CHOOSE_CITY("Введите имя и выберите город", Type.INPUT),
  SET_MENU("Установить меню", Type.INPUT),
  SET_CLEAR_MENU("Удалить меню", Type.INPUT),
  SET_CANCEL("Отменить", Type.INPUT),

  // --- Стейты-действия (кнопки меню) ---
  PROFILE_MENU("👤 Профиль", Type.ACTION),
  BACK_TO_MENU("⬅️ Назад", Type.ACTION),
  CHANGE_NAME_ONLY("Изменить имя", Type.ACTION),
  CHANGE_CITY_ONLY("Изменить город", Type.ACTION),
  SET_NAME_ONLY("Введите новое имя ✏️", Type.INPUT),
  SEND_MESSAGE_TO_ALL_USERS("Введите сообщение для рассылки", Type.ACTION),
  WHO_WILL_COME_TO_OFFICE("Посмотреть кто придет в офис", Type.ACTION),
  SET_OFFICE_ATTENDANCE("Пойду ли я в офис", Type.ACTION),
  VIEW_POOL("Общий пул обедов", Type.ACTION),

  CREATE_ORDER("Сделать заказ", Type.ACTION),
  GET_ORDER("Посмотреть заказ", Type.ACTION),
  CHANGE_ORDER("Изменить заказ", Type.ACTION),
  DELETE_ORDER("Удалить заказ", Type.ACTION),
  SHARE_LUNCH("Поделиться обедом", Type.ACTION),
  RANDOM_ORDER("Рандомный заказ", Type.ACTION),
  SUBMIT_ORDER("Подтвердить заказ", Type.ACTION),

  GET_TODAY_ORDERS("Выгрузить заказы", Type.ACTION),
  GET_ATTENDANCE_STATS("Общая статистика посещений", Type.ACTION),
  GET_ATTENDANCE_STATS_MONTH("Статистика посещений за месяц", Type.ACTION),
  PUBLISH_MENU("Опубликовать меню", Type.ACTION),
  CREATE_MENU("Создать меню", Type.ACTION),
  CLEAR_MENU("Очистить меню", Type.ACTION),
  CHANGE_MENU("Изменить меню", Type.ACTION),
  SEND_FEEDBACK("Отправить фидбэк", Type.INPUT),
  CHANGE_DEADLINE("Изменить дедлайн меню", Type.INPUT);
  //  TEMP_ORDER_FOR_USER("Заказать для сотрудника", Type.ACTION);

  private final String displayName;
  private final Type type; // Добавляем поле для типа стейта

  State(String displayName, Type type) {
    this.displayName = displayName;
    this.type = type;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Type getType() {
    return type;
  }

  // Вложенный enum для типов стейтов
  public enum Type {
    ACTION, // Стейты, которые инициируют действие (обычно кнопки)
    INPUT, // Стейты, которые ожидают ввода данных от пользователя
    INTERNAL // Внутренние стейты, которые не показываются пользователю напрямую, но используются
    // для логики
  }
}
