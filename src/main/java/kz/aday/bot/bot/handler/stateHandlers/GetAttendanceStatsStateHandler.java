/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.util.Messages;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetAttendanceStatsStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.GET_ATTENDANCE_STATS.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);

    if (optionalUser.isPresent()) {
      User user = optionalUser.get();

      if (checkAdminRole(user, getMessageId(update), sender)) {
        return;
      }

      sendMessage(
          user,
          Messages.REPORT_MESSAGE_OVERALL_ATTENDANCE
              + officeAttendanceService.getOverallAttendanceStats(user.getCity()),
          getMessageId(update),
          sender);
    }
  }
}
