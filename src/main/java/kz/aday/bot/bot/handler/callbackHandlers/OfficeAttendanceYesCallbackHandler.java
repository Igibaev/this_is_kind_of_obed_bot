/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.messages.Messages;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

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
      officeAttendanceService.save(user.getId(), user.getPreferedName(), user.getCity(), true);
      sendMessage(user, Messages.THANKS_WILL_COME, getMessageId(callback), sender);
    }
  }
}
