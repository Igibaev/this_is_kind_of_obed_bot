package kz.aday.bot.bot.handler.stateHandlers;
public enum State {
    // --- Специальные/технические стейты ---
    NONE("", Type.INTERNAL), // Начальное или неопределенное состояние
    DEFAULT("", Type.INTERNAL), // Состояние по умолчанию, возможно, после какого-то действия

    // --- Стейты, требующие ввода данных ---
    CHOOSE_CITY("Выберите город", Type.INPUT),
    SET_USERNAME_THEN_CHOOSE_CITY("Введите имя и выберите город", Type.INPUT),
    INPUT_MESSAGE_TO_ALL_USERS("Введите сообщение для рассылки", Type.INPUT), // Переименовано для ясности, что это ввод

    // --- Стейты-действия (кнопки меню) ---
    PROFILE("Профиль", Type.ACTION),
    GET_ALL_ORDERS("Выгрузить заказы", Type.ACTION),
    PUBLISH_MENU("Опубликовать меню", Type.ACTION),
    CREATE_MENU("Создать меню", Type.ACTION),
    CREATE_ORDER("Сделать заказ", Type.ACTION),
    CLEAR_MENU("Очистить меню", Type.ACTION),
    CHANGE_MENU("Изменить меню", Type.ACTION),
    TEMP_ORDER_FOR_USER("Заказать для сотрудника", Type.ACTION),
    GET_ORDER("Посмотреть заказ", Type.ACTION),
    CHANGE_ORDER("Изменить заказ", Type.ACTION),
    DELETE_ORDER("Удалить заказ", Type.ACTION),
    RANDOM_ORDER("Рандомный заказ", Type.ACTION),
    EDIT_USERNAME("Изменить имя и город", Type.ACTION),
    SUBMIT_ORDER("Потвердить заказ", Type.ACTION);


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
        INPUT,  // Стейты, которые ожидают ввода данных от пользователя
        INTERNAL // Внутренние стейты, которые не показываются пользователю напрямую, но используются для логики
    }
}