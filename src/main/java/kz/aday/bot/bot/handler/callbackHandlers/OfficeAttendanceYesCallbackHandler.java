/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class OfficeAttendanceYesCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.ATTENDANCE_YES);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);

    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      String[] data = callback.getData().split(":");
      boolean isToday = data.length > 1 && "TODAY".equals(data[1]);
      LocalDate date = isToday ? LocalDate.now() : LocalDate.now().plusDays(1);
      officeAttendanceService.save(
          user.getId(), user.getPreferedName(), user.getCity(), true, date);
      sendMessage(
          user,
          isToday
              ? Messages.THANKS_WILL_COME_TODAY.getText()
              : Messages.THANKS_WILL_COME_TOMORROW.getText(),
          getMessageId(callback),
          sender);
    }
  }
}
