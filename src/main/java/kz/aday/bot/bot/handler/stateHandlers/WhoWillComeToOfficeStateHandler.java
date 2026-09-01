/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class WhoWillComeToOfficeStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.WHO_WILL_COME_TO_OFFICE.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      user.setState(State.NONE);
      userService.save(user);
      if (user.getCity().isNextDayOrderCycle()) {
        List<UserButton> buttons =
            List.of(
                new UserButton(
                    CallbackState.WHO_COMES_TODAY_ALMATA.getDisplayName(),
                    CallbackState.WHO_COMES_TODAY_ALMATA.name()),
                new UserButton(
                    CallbackState.WHO_COMES_TOMORROW_ALMATA.getDisplayName(),
                    CallbackState.WHO_COMES_TOMORROW_ALMATA.name()));
        sendMessageWithKeyboard(
            user,
            CHOOSE_DATE_MESSAGE,
            KeyboardUtil.createInlineKeyboard(buttons),
            getMessageId(update),
            sender);
      } else {
        List<Order> orders =
            orderService.findAll().stream()
                .filter(o -> o.getCity() == user.getCity())
                .filter(o -> o.getStatus() == Status.READY)
                .collect(Collectors.toList());
        String names = orders.stream().map(Order::getUsername).collect(Collectors.joining(", "));
        if (names.isBlank()) {
          sendMessage(user, NOBODY_COMES_TODAY, getMessageId(update), sender);
        } else {
          sendMessage(
              user, String.format(WHO_COMES, orders.size(), names), getMessageId(update), sender);
        }
      }
    }
  }

  static final String NOBODY_COMES_TODAY = "Сегодня никто в офис не придет( хнык хнык";
  static final String WHO_COMES = "Список людей кто придет в офис: [%s]\n%s";
  private static final String CHOOSE_DATE_MESSAGE = "Выберите за какой день посмотреть:";
}
