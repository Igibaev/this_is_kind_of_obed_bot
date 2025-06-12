package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.model.City;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SetUsernameStateHandler extends StateHandler {
    @Override
    public boolean canHandle(String state) {
        return State.SET_USERNAME_THEN_CHOOSE_CITY.toString().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        user.setPreferedName(update.getMessage().getText());
        user.setState(State.CHOOSE_CITY);
        sendMessage(
                user,
                String.format(SET_NAME_MESSAGE, update.getMessage().getText()),
                KeyboardUtil.createReplyKeyboard(
                        Arrays.stream(City.values())
                                .map(City::getValue)
                                .collect(Collectors.toList())
                ),
                getMessageId(update),
                sender
        );
    }

    private static final String SET_NAME_MESSAGE =
            "Сохранил имя %s. Теперь выбери город.";
}
