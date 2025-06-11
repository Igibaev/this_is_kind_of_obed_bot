package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

import static kz.aday.bot.model.User.Role.ADMIN;

public class DefaultStateHandler extends StateHandler {
    @Override
    public boolean canHandle(String state) {
        return State.DEFAULT.toString().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        sendMessage(user, NAVIGATION_MENU, getUserMenuKeyboard(user), sender);
    }

    private ReplyKeyboard getUserMenuKeyboard(User user) {
        boolean isMenuExist = isMenuExist(user.getCity());
        boolean isMenuReady = isMenuReady(user.getCity());
        boolean isOrderExist = isOrderExist(user);
        boolean isOrderReady = isOrderReady(user);

        List<String> userMenuItems = new ArrayList<>();
        userMenuItems.add(CallbackState.PROFILE.getDisplayName());
        userMenuItems.add(CallbackState.EDIT_USERNAME.getDisplayName());
        userMenuItems.add(CallbackState.TEMP_ORDER_FOR_USER.getDisplayName());

        if (isMenuExist && isMenuReady) {
            if (isOrderExist) {
                if (isOrderReady) {
                    userMenuItems.add(CallbackState.DELETE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.GET_ORDER.getDisplayName());
                } else {
                    userMenuItems.add(CallbackState.SUBMIT_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.GET_ORDER.getDisplayName());
                }
            } else {
                userMenuItems.add(CallbackState.CREATE_ORDER.getDisplayName());
            }
        }

        if (user.getRole() == ADMIN) {
            userMenuItems.add(CallbackState.INPUT_MESSAGE_TO_ALL_USERS.getDisplayName());
            userMenuItems.add(CallbackState.GET_ALL_ORDERS.getDisplayName());
            if (isMenuExist) {
                if (isMenuReady) {
                    userMenuItems.add(CallbackState.CLEAR_MENU.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_MENU.getDisplayName());
                } else {
                    userMenuItems.add(CallbackState.PUBLISH_MENU.getDisplayName());
                }
            } else {
                userMenuItems.add(CallbackState.CREATE_MENU.getDisplayName());
            }
        }
        return KeyboardUtil.createReplyKeyboard(userMenuItems);
    }

    private static final String NAVIGATION_MENU =
            "Меню навигации по боту.";
}
