package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.model.City;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SetCityStateHandler extends StateHandler {
    @Override
    public boolean canHandle(String state) {
        return State.CHOOSE_CITY.toString().equals(state);
    }

    @Override
    public void handle(Update update, AbsSender sender) throws Exception {
        User user = userService.findById(getChatId(update).toString());
        City city = City.from(update.getMessage().getText());
        user.setCity(city);
        user.setState(State.NONE);
        sendMessage(user, SET_CITY_MESSAGE, getMessageId(update), sender);
    }

    private static final String SET_CITY_MESSAGE =
            "Прекрасный город. Нажми /return чтобы вернуться в меню навигации.";
}
