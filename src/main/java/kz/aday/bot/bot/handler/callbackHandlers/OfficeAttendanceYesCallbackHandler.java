/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.messages.Messages;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.LocalDate;
import java.util.Optional;

public class OfficeAttendanceYesCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    String[] data = callback.getData().split(":");
    if (data.length <= 0) {
      throw new IllegalArgumentException("There is no callback");
    }

    return CallbackState.ATTENDANCE_YES.name().equals(data[0]);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
      Optional<User> optionalUser = findReadyUserByChatId(callback);

    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      String[] data = callback.getData().split(":");
      boolean isToday = data.length > 1 && "TODAY".equals(data[1]);
      LocalDate date = isToday ? LocalDate.now() : LocalDate.now().plusDays(1);
      officeAttendanceService.save(user.getId(), user.getPreferedName(), user.getCity(), true, date);
      sendMessage(
          user,
          isToday ? Messages.THANKS_WILL_COME_TODAY : Messages.THANKS_WILL_COME_TOMORROW,
          getMessageId(callback),
          sender);
    }
  }
}
