/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class WhoWillComeToOfficeStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.WHO_WILL_COME_TO_OFFICE.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      user.setState(State.NONE);
      String usersWhoWillComeToOffice =
          orderService.findAll().stream()
              .filter(o -> o.getCity() == user.getCity())
              .filter(o -> o.getStatus() == Status.READY)
              .map(Order::getUsername)
              .collect(Collectors.joining(", "));
      if (usersWhoWillComeToOffice.isBlank()) {
        sendMessage(
            user, "Сегодня никто в офис не придет( хнык хнык", getMessageId(update), sender);
      } else {
        sendMessage(
            user,
            String.format(THESE_ARE_USERS_WHO_ORDERED, usersWhoWillComeToOffice),
            getMessageId(update),
            sender);
      }
    }
  }

  private final String THESE_ARE_USERS_WHO_ORDERED = "Список людей кто придет в офис: \n[%s]";
}
