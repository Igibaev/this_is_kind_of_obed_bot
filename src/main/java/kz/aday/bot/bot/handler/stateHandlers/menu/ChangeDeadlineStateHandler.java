/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.menu;

import java.time.LocalDateTime;
import java.util.List;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.service.MenuTextParser;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ChangeDeadlineStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.CHANGE_DEADLINE.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    if (isUserExistAndReady(update)) {
      User user = userService.findById(getChatId(update).toString());
      if (user.getRole() == User.Role.USER) {
        sendMessage(user, PERMISSION_DENIED, getMessageId(update), sender);
      }
      LocalDateTime newDeadline = MenuTextParser.parseDeadline(update.getMessage().getText());
      Menu menu = menuService.findById(user.getCity().toString());
      menu.setDeadline(newDeadline);
      menuService.save(menu);

      InlineKeyboardMarkup markup =
          KeyboardUtil.createInlineKeyboard(menu.getItemList(), CallbackState.NONE);
      KeyboardUtil.addButton(
          List.of(
              new UserButton("Опубликовать меню", CallbackState.SUBMIT_MENU.toString()),
              new UserButton("Изменить меню", CallbackState.CHANGE_MENU.toString()),
              new UserButton("Удалить меню", CallbackState.CLEAR_MENU.toString())),
          markup);
      sendMessageWithKeyboard(
          user,
          String.format(
              DEADLINE_IS_SET_AND_MENU_IS_PUBLISHE,
              user.getCity().getValue(),
              menu.getDeadlineAsText()),
          markup,
          getMessageId(update),
          sender);
    }
  }

  private static final String PERMISSION_DENIED = "Нет доступа.";

  private static final String DEADLINE_IS_SET_AND_MENU_IS_PUBLISHE =
      "Скорректировали дедлайн для меню.\n" + "Вот меню для *%s* \n" + "Дедлайн *%s*\n";
}
