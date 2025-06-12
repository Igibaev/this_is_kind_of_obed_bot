package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.City;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.HashMap;
import java.util.Map;

public class CreateMenuStateHandler extends AbstractHandler implements StateHandler {
    @Override
    public boolean canHandle(String state) {
        return State.CREATE_MENU.getDisplayName().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        if (isUserExistAndReady(update)) {
            User user = userService.findById(getChatId(update).toString());
            if (user.getRole() == User.Role.USER) {
                sendMessage(user, PERMISSION_DENIED, sender);
            } else {
                user.setState(State.SET_MENU);
                sendMessage(user, MENU_TEMPLATE, sender);
            }
        }
    }

    private static final String PERMISSION_DENIED = "Нет доступа к созданию меню";

    private static final String MENU_TEMPLATE =
            "Шаблон меню:\n" +
            "Возможны только 5 категории блюд(первое, второе, салат, выпечка, хлеб)\n" +
            "\n" +
            "Первое:\n" +
            "Блюда перечисляются через отступ строки\n" +
            "\n" +
            "Второе:\n" +
            "Блюдо 1\n" +
            "Блюдо 2\n" +
            "\n" +
            "Салат:\n" +
            "1. Блюдо('1. ' это затираться будет)\n" +
            "\n" +
            "Выпечка:\n" +
            "\n" +
            "Хлеб: (если хлеба нету,  либо он всегда к заказу идет, то лучше его убрать)\n" +
            "\n" +
            "Дедлайн 11:00. (дедлайн можно указывать так, можно просто время указывать в формате HH:mm)\n" +
            "\n" +
            "Чтобы отменить нажми /cancel\n";
}
