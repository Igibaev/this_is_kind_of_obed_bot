/* (C) 2024 Igibaev */
package kz.aday.bot.util;

public enum Messages {
  RETURN_TO_MENU("Чтобы вернуться в меню нажмите /cancel"),
  DEADLINE_IS_NEAR_MAKE_AN_ORDER("Скоро дедлайн, успей заказать еду."),
  MENU_IS_CLOSED("Меню для заказов закрыто"),

  WILL_COME_DAY_QUESTION("За какой день хочешь отметиться?"),
  WILL_COME_TODAY_QUESTION("Пойдешь ли ты сегодня в офис?"),
  WILL_COME_TOMORROW_QUESTION("Пойдешь ли ты завтра в офис?"),
  THANKS_WILL_COME_TODAY("Отлично! Записал: сегодня ты будешь в офисе."),
  THANKS_WILL_COME_TOMORROW("Отлично! Записал: завтра ты будешь в офисе."),
  THANKS_WONT_COME_TODAY("Понял, сегодня тебя в офисе не будет."),
  THANKS_WONT_COME_TOMORROW("Понял, завтра тебя в офисе не будет."),

  MENU_TEMPLATE(
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
          + "Чтобы отменить нажми /cancel\n"),

  PERMISSION_DENIED("Нет доступа."),
  OK("Ok"),

  NAVIGATION_MENU("Меню навигации по боту."),
  RETURNING_NAVIGATION_MENU("Возвращаемся. Меню навигации по боту."),
  OK_RETURN_TO_MENU("Окей. Вернитесь в меню тогда /return"),

  MENU_DEADLINE_IS_PASSED("Дедлайн уже прошел."),
  MENU_IS_NOT_READY_TODAY("Меню на сегодня еще не готово. /return"),
  CHOOSE_ITEM_MESSAGE("Выберите что хотите заказать:"),
  CREATING_ORDER_MESSAGE("Собираем ваш заказ."),

  YOUR_ORDER_IS("Твой заказ %s."),
  YOUR_ORDER_IS_TODAY("Твой заказ на сегодня %s."),
  YOUR_ORDER_IS_TOMORROW("Твой заказ на завтра %s."),
  ORDER_IS_EMPTY("Твой заказ пуст."),
  ORDER_IS_EMPTY_TODAY("Заказ на сегодня не найден."),
  ORDER_IS_EMPTY_TOMORROW("Заказ на завтра не найден."),
  ORDER_DELETED_RETURN("Ваш заказ удален./return"),
  ORDER_WAS_DELETED("Ваш заказ удален."),
  ORDER_SENDED("Ваш заказ улетел.%s./return"),
  ORDER_WAS_SUBMITED("Твой заказ %s. Подтвержден."),
  SUBMIT_ORDER_CONFIRM_PROMPT("Твой заказ %s. Чтобы подтвердить отправьте 'Да'."),
  SUBMIT_ORDER_MENU_DEADLINE_PASSED("Дедлайн меню уже прошел."),
  YOUR_ORDER_IS_DELETE_CONFIRM("Твой заказ %s. Чтобы удалить отправьте 'Да'."),
  MENU_DEADLINE_IS_PASSED_ORDER_SENT("Дедлайн уже прошел, заказ отправлен."),

  CHOOSE_DATE_MESSAGE_ORDER("Выберите за какой день посмотреть заказ:"),
  CHOOSE_DATE_MESSAGE_TODAY_ORDERS("Выберите за какой день выгрузить заказы:"),
  CHOOSE_DATE_MESSAGE_WHO_COMES("Выберите за какой день посмотреть:"),
  EMPTY_ORDERS("Список заказов пуст."),
  EMPTY_ORDERS_TODAY("Список заказов на сегодня пуст."),
  EMPTY_ORDERS_TOMORROW("Список заказов на завтра пуст."),
  REPORT_MESSAGE_ORDERS_LIST("Список заказов.\n"),
  REPORT_MESSAGE_ORDERS_TODAY("Заказы на сегодня.\n"),
  REPORT_MESSAGE_ORDERS_TOMORROW("Заказы на завтра.\n"),
  REPORT_MESSAGE_OVERALL_ATTENDANCE("Общая статистика посещений:\n"),
  REPORT_MESSAGE_MONTH_ATTENDANCE("Статистика посещений за текущий месяц:\n"),

  NOBODY_COMES_TODAY("Сегодня никто в офис не придет( хнык хнык"),
  NOBODY_COMES_TOMORROW("Завтра никто в офис не придет( хнык хнык"),
  WHO_COMES_TODAY("Сегодня в офис придет [%s]:\n%s"),
  WHO_COMES_TOMORROW("Завтра в офис придет [%s]:\n%s"),
  WHO_COMES_OFFICE("Список людей кто придет в офис: [%s]\n%s"),

  CANCEL_RANDOM("Ну ладно, выбери сам. /return"),
  RANDOM_ORDER_MESSAGE("Вы хотите рандомно сделать заказ?"),
  RANDOM_ORDER_CREATED_MESSAGE("Ваш заказ улетел. Пусть содержимое заказа останется тайной. пока."),

  PROFILE_MESSAGE("Ваше имя: %s \nГород: %s \nВернуться в меню /return"),

  DEADLINE_IS_SET_AND_MENU_IS_PUBLISHE(
      "Скорректировали дедлайн для меню.\n" + "Вот меню для *%s* \n" + "Дедлайн *%s*\n"),
  MENU_TO_DELETE("Вы хотите удалить меню на сегодня?\n"),
  MENU_NOT_EXIST_FOR_CITY(
      "Меню не создано для города *%s*\n" + "Нажмите /return чтобы вернутся в меню.\n"),
  MENU_NOT_EXIST("Меню не создано, сначала создайте меню. Чтобы вернуться нажмите \return"),
  PUBLISH_MENU_READY_MESSAGE(
      "Вот меню для города *%s*. Оно уже опубликовано. \n"
          + "Дедлайн до: *%s* \n"
          + "Чтобы отменить нажми /cancel"),
  PUBLISH_MENU_MESSAGE(
      "Вот меню для города *%s*.\n" + "Дедлайн до: *%s* \n" + "Чтобы отменить нажми /cancel"),
  MENU_PENDING("Проверьте корректность меню для города %s.\nЧтобы отменить нажми /cancel"),
  CREATE_MENU_READY_MESSAGE(
      "Вот меню для города *%s*.  Дедлайн до *%s*. \n Оно уже опубликовано. \n"
          + "Чтобы отменить нажми /cancel"),
  CREATE_MENU_PENDING_MESSAGE(
      "Вот меню для города *%s*. Дедлайн до *%s*. \n Но оно не опубликовано.\n"
          + "Чтобы отменить нажми /cancel"),
  NOTIFY_IF_USER_HAS_BANNED_ITEMS(
      "Меню изменилось. Из вашего заказа были убраны след позиции %s. "
          + "Выберите то что осталось в меню. "
          + "Или нажмите /cancel чтобы оставить заказ без изменений или удалите его /delete."),
  MENU_IS_PUBLISHED("Меню опубликовали."),
  MENU_IS_ALREADY_EXPIRED(
      "У меню: *%s* \nдедлайн уже прошел *%s*.\n" + "Введите время дедлайна в формате HH:MM"),
  NEW_MENU_IS_PUBLISHED(
      "Новое меню доступно для заказа.\n" + "Город: *%s* \n" + "Дедлайн до: *%s* "),
  MENU_WAS_DELETED("Меню и все заказы были удалены."),

  CURRENT_PENDING_ORDER("Ты не закончил заказ."),
  CURRENT_ORDER("Твой заказ."),
  MENU_TODAY(
      "Город: *%s*. Вот что сегодня в меню! \n"
          + "Дедлайн до: *%s* \n"
          + "Чтобы отменить заказ нажми /cancel"),
  START_MESSAGE_INPUT_NAME(
      "Добро пожаловать! \n"
          + "Данный бот предназначен \n"
          + "для заказов еды в koronaTech.\n"
          + "Введите своё имя. \n"
          + "Чтобы отменить нажми /cancel"),

  SET_CITY_MESSAGE("Прекрасный город. Нажми /return чтобы вернуться в меню навигации."),
  CITY_DOESN_EXIST_YET("%s город не заведен в систему, выберите те которые вам предложены."),
  CITY_IS_NULL("Вы не выбрали город. Выберите город."),
  SET_NAME_MESSAGE("Сохранил имя %s. Теперь выбери город."),
  CHANGE_NAME_MESSAGE("Введи новое имя \n" + "Чтобы отменить нажми /cancel"),

  FEEDBACK_FORM_MESSAGE(
      "Это форма обратной связи. Приложите фотографии, аудио, видео или просто текстом опишите, ваши пожелания как сделать чат бот лучше и удобнее."),
  ATTENDANCE_SHEET("Отчет посещяемости офиса %s за 30дней\n%s"),
  LOGS_FILE_NOT_FOUND("Не удалось найти или сформировать файл логов с запрошенными строками."),
  LOGS_FILE_ERROR("Произошла ошибка при подготовке файла логов."),
  BROADCAST_SUCCESS("Рассылка прошла успешно."),
  INPUT_MESSAGE(
      "Введите сообщение для рассылки, для города %s. \n" + "Чтобы отменить нажмите /cancel"),

  ERROR_MESSAGE("Произошла ошибка: %s."),
  GO_TO_START_COMMAND("Чтобы начать взаимодейcтвовать с ботом, завершите команду /start.");

  private final String text;

  Messages(String text) {
    this.text = text;
  }

  public String getText(Object... args) {
    return args.length == 0 ? text : String.format(text, args);
  }

  @Override
  public String toString() {
    return text;
  }
}
