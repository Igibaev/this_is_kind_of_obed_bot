/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.messages.Messages;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SetOfficeAttendanceStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.SET_OFFICE_ATTENDANCE.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      user.setState(State.NONE);
      sendMessageWithKeyboard(
          user,
          Messages.WILL_COME_DAY_QUESTION,
          KeyboardUtil.createInlineKeyboard(
              List.of(
                  new UserButton("Сегодня", CallbackState.ATTENDANCE_DAY_TODAY.name()),
                  new UserButton("Завтра", CallbackState.ATTENDANCE_DAY_TOMORROW.name()))),
          getMessageId(update),
          sender);
    }
  }
}
