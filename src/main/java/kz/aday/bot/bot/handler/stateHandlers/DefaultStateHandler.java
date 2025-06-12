package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class DefaultStateHandler extends AbstractHandler implements StateHandler {
    @Override
    public boolean canHandle(String state) {
        return State.DEFAULT.getDisplayName().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        sendMessageWithKeyboard(user, NAVIGATION_MENU, getUserMenuKeyboard(user), getMessageId(update), sender);
    }

    private static final String NAVIGATION_MENU = "Меню навигации по боту.";
}
