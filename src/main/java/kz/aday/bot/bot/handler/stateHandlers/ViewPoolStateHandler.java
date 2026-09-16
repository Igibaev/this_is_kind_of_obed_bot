/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class ViewPoolStateHandler extends AbstractHandler implements StateHandler {
  @Override
  public boolean canHandle(String state) {
    return State.VIEW_POOL.getDisplayName().equals(state);
  }

  @Override
  public void handle(Update update, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(update);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      user.setState(State.NONE);
      userService.save(user);

      LocalDate targetDate = user.getCity().getCurrentOrderDate();
      List<SharedOrderItem> entries =
          sharedOrderItemPoolService.getAvailableEntries(user.getCity(), targetDate);
      if (entries.isEmpty()) {
        sendMessage(user, Messages.POOL_EMPTY.getText(), getMessageId(update), sender);
      } else {
        InlineKeyboardMarkup keyboard =
            KeyboardUtil.createPoolInlineKeyboard(entries, CallbackState.POOL_CLAIM);
        KeyboardUtil.addButton(
            List.of(new UserButton("Назад", CallbackState.CANCEL.toString())), keyboard);
        sendMessageWithKeyboard(
            user, Messages.POOL_HEADER.getText(), keyboard, getMessageId(update), sender);
      }
    }
  }
}
