/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.messages.Messages;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class AttendanceDayTodayCallbackHandler extends AbstractHandler implements CallbackHandler {

  private static final String TODAY = ":TODAY";
  @Override
  public boolean canHandle(CallbackQuery callback) {
    String[] data = callback.getData().split(":");
    if (data.length <= 0) {
      throw new IllegalArgumentException("There is no callback");
    }

    return CallbackState.ATTENDANCE_DAY_TODAY.name().equals(data[0]);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);

    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      sendMessageWithKeyboard(
          user,
          Messages.WILL_COME_TODAY_QUESTION,
          KeyboardUtil.createInlineKeyboard(
              List.of(
                  new UserButton("Да", CallbackState.ATTENDANCE_YES.name() + TODAY),
                  new UserButton("Нет", CallbackState.ATTENDANCE_NO.name() + TODAY))),
          getMessageId(callback),
          sender);
    }
  }
}
