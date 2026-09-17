/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
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
                    CallbackState.WHO_COMES_TODAY.getDisplayName(),
                    CallbackState.WHO_COMES_TODAY.name()),
                new UserButton(
                    CallbackState.WHO_COMES_TOMORROW.getDisplayName(),
                    CallbackState.WHO_COMES_TOMORROW.name()));
        sendMessageWithKeyboard(
            user,
            Messages.CHOOSE_DATE_WHO_COMES.getText(),
            KeyboardUtil.createInlineKeyboard(buttons),
            getMessageId(update),
            sender);
      } else {
        sendWhoComes(
            user,
            LocalDate.now(),
            Messages.WHO_COMES_TODAY,
            Messages.NOBODY_COMES_TODAY,
            getMessageId(update),
            sender);
      }
    }
  }
}
