/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class GetOrdersTomorrowCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.GET_ORDERS_TOMORROW);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (checkAdminRole(user, getMessageId(callback), sender)) {
        return;
      }

      sendOrdersReport(
          user,
          LocalDate.now().plusDays(1),
          Messages.EMPTY_ORDERS_TOMORROW,
          Messages.REPORT_ORDERS_TOMORROW,
          getMessageId(callback),
          sender);
    }
  }
}
