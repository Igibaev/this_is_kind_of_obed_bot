package kz.aday.bot.bot.handler.callbackHandlers;

import lombok.Getter;

public enum CallbackState {
    ADD_ITEM_TO_ORDER("Добавить пункт в заказ"),
    INPUT_MESSAGE_TO_ALL_USERS("Отправить сообщение всем"),
    PROFILE("Профиль"),
    GET_ALL_ORDERS("Выгрузить заказы"),
    PUBLISH_MENU("Опубликовать меню"),
    CREATE_MENU("Создать меню"),
    CREATE_ORDER("Сделать заказ"),
    CLEAR_MENU("Очистить меню"),
    CHANGE_MENU("Изменить меню"),
    TEMP_ORDER_FOR_USER("Заказать для сотрудника"),
    GET_ORDER("Посмотреть заказ"),
    CHANGE_ORDER("Изменить заказ"),
    DELETE_ORDER("Удалить заказ"),
    RANDOM_ORDER("Рандомный заказ"),
    EDIT_USERNAME("Изменить имя и город"),

    CANCEL("Отменить/Вернуться"),

    SUBMIT("Потвердить"),
    SUBMIT_ORDER("Потвердить заказ");

    @Getter
    private String displayName;

    CallbackState(String state) {
        this.displayName = state;
    }
}
