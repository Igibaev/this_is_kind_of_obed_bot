/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class AttendanceDayTomorrowCallbackHandler extends AbstractHandler
    implements CallbackHandler {

  private static final String TOMORROW = ":TOMORROW";

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.ATTENDANCE_DAY_TOMORROW);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);

    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      sendMessageWithKeyboard(
          user,
          Messages.WILL_COME_TOMORROW_QUESTION.getText(),
          KeyboardUtil.createInlineKeyboard(
              List.of(
                  new UserButton("Да", CallbackState.ATTENDANCE_YES.name() + TOMORROW),
                  new UserButton("Нет", CallbackState.ATTENDANCE_NO.name() + TOMORROW))),
          getMessageId(callback),
          sender);
    }
  }
}
