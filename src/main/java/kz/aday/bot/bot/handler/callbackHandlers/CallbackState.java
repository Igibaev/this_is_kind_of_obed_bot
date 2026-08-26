/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import lombok.Getter;

public enum CallbackState {
  NONE(""),
  ADD_ITEM_TO_ORDER("Добавить пункт в заказ"),

  CANCEL("Отменить/Вернуться"),

  SUBMIT_MENU("Опубликовать меню"),
  CHANGE_MENU("Изменить меню"),
  CLEAR_MENU("Очистить меню"),

  SUBMIT_TEMP_ORDER("Подтвердить временный заказ"),
  SUBMIT_ORDER("Подтвердить заказ"),
  CHANGE_ORDER("Изменить заказ"),
  DELETE_ORDER("Удалить заказ"),

  GET_ORDER_TODAY_ALMATA("Заказ на сегодня"),
  GET_ORDER_TOMORROW_ALMATA("Заказ на завтра"),
  GET_ORDERS_TODAY_ALMATA("Заказы на сегодня"),
  GET_ORDERS_TOMORROW_ALMATA("Заказы на завтра"),
  WHO_COMES_TODAY_ALMATA("Кто приходит сегодня"),
  WHO_COMES_TOMORROW_ALMATA("Кто придет завтра"),

  ATTENDANCE_DAY_TODAY("Сегодня"),
  ATTENDANCE_DAY_TOMORROW("Завтра"),
  ATTENDANCE_YES("Да, приду"),
  ATTENDANCE_NO("Нет, не приду");

  @Getter private String displayName;

  CallbackState(String state) {
    this.displayName = state;
  }
}
