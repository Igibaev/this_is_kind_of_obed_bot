package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface StateHandler {

    default boolean canHandle(String state, User user) {
        if (user == null || user.getState() == State.NONE) {
            return canHandle(state);
        }
        return canHandle(user.getState().toString());
    }

    boolean canHandle(String state);

    void handle(Update update, AbsSender sender) throws Exception;

}
