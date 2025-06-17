package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class CancelCallbackHandler extends AbstractHandler implements CallbackHandler {
    @Override
    public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
        if (isUserExistAndReady(callback)) {
            User user = userService.findById(getChatId(callback).toString());
            user.setState(State.NONE);
            sendMessageWithKeyboard(user, NAVIGATION_MENU, getUserMenuKeyboard(user), getMessageId(callback), sender);
        }
    }

    @Override
    public boolean canHandle(CallbackQuery callback) {
        String[] data = callback.getData().split(":");
        if (data.length <= 0) {
            throw new IllegalArgumentException("There is no callback");
        }
        return CallbackState.CANCEL.toString().equals(data[0]);
    }

    private static final String NAVIGATION_MENU = "Меню навигации по боту.";

}
