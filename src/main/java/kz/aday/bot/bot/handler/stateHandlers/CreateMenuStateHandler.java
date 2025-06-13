package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

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
                sendMessage(user, PERMISSION_DENIED, getMessageId(update),sender);
            } else {
                if (isMenuExist(user.getCity())) {
                    Menu menu = menuService.findById(user.getCity().toString());
                    if (menu.getStatus() == Status.READY) {
                        InlineKeyboardMarkup markup = KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
                        KeyboardUtil.addButton(
                                List.of(
                                        new UserButton("Изменить меню", CallbackState.CHANGE_MENU.toString())
                                ),
                                markup
                        );
                        sendMessageWithKeyboard(
                                user,
                                String.format(MENU_READY_MESSAGE, user.getCity().getValue()),
                                markup,
                                getMessageId(update),
                                sender
                        );
                    } else {
                        InlineKeyboardMarkup markup = KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
                        KeyboardUtil.addButton(
                                List.of(
                                        new UserButton("Опубликовать меню", CallbackState.SUBMIT_MENU.toString()),
                                        new UserButton("Изменить меню", CallbackState.CHANGE_MENU.toString())
                                ),
                                markup
                        );
                        sendMessageWithKeyboard(
                                user,
                                String.format(MENU_PENDING_MESSAGE, user.getCity().getValue()),
                                markup,
                                getMessageId(update),
                                sender
                        );
                    }
                } else {
                    user.setState(State.SET_MENU);
                    sendMessage(user, MENU_TEMPLATE, getMessageId(update),sender);
                }
            }
        }
    }

    private static final String PERMISSION_DENIED = "Нет доступа к созданию меню";

    private static final String MENU_READY_MESSAGE =
            "Вот меню для города *%s*. Оно уже опубликовано. \n" +
            "Чтобы отменить нажми /cancel";

    private static final String MENU_PENDING_MESSAGE =
            "Вот меню для города *%s*. Но оно не опубликовано.\n" +
            "Чтобы отменить нажми /cancel";

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
