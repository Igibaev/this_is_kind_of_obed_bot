/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ClearMenuCallbackHandler extends AbstractHandler implements CallbackHandler {
  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (checkAdminRole(user, getMessageId(callback), sender)) {
        return;
      }
      menuService.deleteById(user.getCity().toString());
      orderService.findAll().stream()
          .filter(order -> order.getCity() == user.getCity())
          .forEach(order -> orderService.deleteById(order.getChatId()));
      sendMessage(user, Messages.MENU_WAS_DELETED.getText(), getMessageId(callback), sender);
    }
  }

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.CLEAR_MENU);
  }
}
