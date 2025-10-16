/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.util.Arrays;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
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

    City city = null;
    try {
      city = City.from(update.getMessage().getText());
    } catch (Exception e) {
      sendMessageWithKeyboard(
          user,
          String.format(CITY_DOESN_EXIST_YET, update.getMessage().getText()),
          KeyboardUtil.createReplyKeyboard(
              Arrays.stream(City.values()).map(City::getValue).collect(Collectors.toList())),
          getMessageId(update),
          sender);
      return;
    }

    if (city == null) {
      sendMessageWithKeyboard(
          user,
          CITY_IS_NULL,
          KeyboardUtil.createReplyKeyboard(
              Arrays.stream(City.values()).map(City::getValue).collect(Collectors.toList())),
          getMessageId(update),
          sender);
      return;
    }

    user.setCity(city);
    user.setState(State.NONE);
    user.setStatus(Status.READY);
    sendMessage(user, SET_CITY_MESSAGE, getMessageId(update), sender);
  }

  private static final String SET_CITY_MESSAGE =
      "Прекрасный город. Нажми /return чтобы вернуться в меню навигации.";

  private static final String CITY_DOESN_EXIST_YET =
      "%s город не заведен в систему, выберите те которые вам предложены.";

  private static final String CITY_IS_NULL = "Вы не выбрали город. Выберите город.";
}
