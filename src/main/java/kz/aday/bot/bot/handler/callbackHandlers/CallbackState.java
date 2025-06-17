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

  SUBMIT_TEMP_ORDER("Потвердить временный заказ"),
  SUBMIT_ORDER("Потвердить заказ"),
  CHANGE_ORDER("Изменить заказ"),
  DELETE_ORDER("Удалить заказ");

  @Getter private String displayName;

  CallbackState(String state) {
    this.displayName = state;
  }
}
