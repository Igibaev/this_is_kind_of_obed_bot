/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SetCityStateHandler extends AbstractHandler implements StateHandler {

  @Override
  public boolean canHandle(String state) {
    return State.CHOOSE_CITY.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    User user = userService.findById(getChatId(update).toString());
    City city = City.from(update.getMessage().getText());
    user.setCity(city);
    user.setState(State.NONE);
    user.setStatus(Status.READY);
    sendMessage(user, SET_CITY_MESSAGE, getMessageId(update), sender);
  }

  private static final String SET_CITY_MESSAGE =
      "Прекрасный город. Нажми /return чтобы вернуться в меню навигации.";
}
