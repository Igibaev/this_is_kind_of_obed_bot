package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ChangeMenuCallbackHanlder extends AbstractHandler implements CallbackHandler {
    @Override
    public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
        if (isUserExistAndReady(callback)) {
            User user = userService.findById(getChatId(callback).toString());
            if (user.getRole() == User.Role.USER) {
                sendMessage(user, PERMISSION_DENIED, getMessageId(callback), sender);
                return;
            }
            user.setState(State.CHANGE_MENU);
            sendMessage(user, MENU_TEMPLATE, getMessageId(callback), sender);
        }
    }

    @Override
    public boolean canHandle(CallbackQuery callback) {
        String[] data = callback.getData().split(":");
        if (data.length <= 0) {
            throw new IllegalArgumentException("There is no callback");
        }
        return CallbackState.CHANGE_MENU.toString().equals(data[0]);
    }

    private static final String PERMISSION_DENIED = "Нет доступа.";

    private static final String MENU_TEMPLATE =
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
