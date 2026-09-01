/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetAttendanceStatsForMonthStateHandler extends AbstractHandler
    implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.GET_ATTENDANCE_STATS_MONTH.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);

    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
        return;
      }

      sendMessage(
          user,
          REPORT_MESSAGE + officeAttendanceService.getCurrentMonthAttendanceStats(user.getCity()),
          getMessageId(update),
          sender);
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";
  private static final String REPORT_MESSAGE = "Статистика посещений за текущий месяц:\n";
}
