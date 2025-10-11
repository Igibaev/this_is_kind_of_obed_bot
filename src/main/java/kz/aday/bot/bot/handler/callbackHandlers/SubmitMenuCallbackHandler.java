/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SubmitMenuCallbackHandler extends AbstractHandler implements CallbackHandler {
  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    if (isUserExistAndReady(callback)) {
      User user = userService.findById(getChatId(callback).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(callback), sender);
        return;
      }
      Menu menu = menuService.findById(user.getCity().toString());
      menu.setStatus(Status.READY);
      menuService.save(menu);
      sendMessage(user, MENU_IS_PUBLISHED, getMessageId(callback), sender);
    }
  }

  @Override
  public boolean canHandle(CallbackQuery callback) {
    String[] data = callback.getData().split(":");
    if (data.length <= 0) {
      throw new IllegalArgumentException("There is no callback");
    }
    return CallbackState.SUBMIT_MENU.toString().equals(data[0]);
  }

  private static final String MENU_IS_PUBLISHED = "Меню опубликовали.";

  private static final String PERMISSION_DENIED = "Нет доступа.";
}
