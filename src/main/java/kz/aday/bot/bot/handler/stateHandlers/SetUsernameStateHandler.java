package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SetUsernameStateHandler extends StateHandler {
    @Override
    public boolean canHandle(String state) {
        return State.SET_USERNAME_THEN_CHOOSE_CITY.toString().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        user.setPreferedName(update.getMessage().getText());
        user.setState(State.DEFAULT);
        sendMessage(user, String.format(SET_NAME_MESSAGE, update.getMessage().getText()), sender);
    }

    private static final String SET_NAME_MESSAGE =
            "Сохранил имя %s.";
}
