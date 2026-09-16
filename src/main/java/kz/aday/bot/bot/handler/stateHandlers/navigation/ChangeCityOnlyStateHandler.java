/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.navigation;

import java.util.Arrays;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.City;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ChangeCityOnlyStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.CHANGE_CITY_ONLY.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    User user = userService.findById(getChatId(update).toString());
    user.setState(State.CHOOSE_CITY);
    sendMessageWithKeyboard(
        user,
        Messages.CHOOSE_CITY_PROMPT.getText(),
        KeyboardUtil.createReplyKeyboard(
            Arrays.stream(City.values()).map(City::getValue).collect(Collectors.toList())),
        getMessageId(update),
        sender);
  }
}
