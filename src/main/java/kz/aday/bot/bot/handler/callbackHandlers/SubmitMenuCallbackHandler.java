/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SubmitMenuCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (checkAdminRole(user, getMessageId(callback), sender)) {
        return;
      }
      Menu menu = menuService.findById(user.getCity().toString());
      if (menu.isDeadlinePassed()) {
        sendMessage(
            user,
            Messages.MENU_IS_ALREADY_EXPIRED.getText(
                menu.getCity().getValue(), menu.getDeadlineAsText()),
            user.getLastMessageId(),
            sender);
        user.setState(State.CHANGE_DEADLINE);
        userService.save(user);
        return;
      }
      menu.setStatus(Status.READY);
      menuService.save(menu);
      sendMessage(user, Messages.MENU_IS_PUBLISHED.getText(), getMessageId(callback), sender);
      for (User userToNotificate :
          userService.findAll().stream().filter(u -> u.getCity() == menu.getCity()).toList()) {
        sendMessageWithKeyboard(
            userToNotificate,
            Messages.NEW_MENU_IS_PUBLISHED.getText(
                    menu.getCity().getValue(),
                    menu.getDeadline().format(DateTimeFormatter.ISO_TIME))
                + "\n\n"
                + menu.getMenuAsFormattedText(),
            KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.ADD_ITEM_TO_ORDER),
            userToNotificate.getLastMessageId(),
            sender);
      }
    }
  }

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.SUBMIT_MENU);
  }
}
