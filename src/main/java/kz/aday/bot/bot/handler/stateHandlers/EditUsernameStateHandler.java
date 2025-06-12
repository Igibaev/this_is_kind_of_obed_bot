package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class EditUsernameStateHandler extends AbstractHandler implements StateHandler {
    @Override
    public boolean canHandle(String state) {
        return State.EDIT_USERNAME.getDisplayName().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        user.setState(State.SET_USERNAME_THEN_CHOOSE_CITY);
        sendMessage(user, CHANGE_NAME_MESSAGE, getMessageId(update), sender);
    }

    private static final String CHANGE_NAME_MESSAGE =
            "Введи новое имя \n" +
            "Чтобы отменить нажми /cancel";
}
