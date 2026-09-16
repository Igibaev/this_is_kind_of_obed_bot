/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.util.KeyboardUtil;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class PoolClaimCallbackHandler extends AbstractHandler implements CallbackHandler {

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.POOL_CLAIM);
  }

  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      String entryId = getEntryId(callback);
      Optional<SharedOrderItem> claimed =
          sharedOrderItemPoolService.claim(user.getCity(), entryId, user.getId(), user.getPreferedName());

      String message;
      if (claimed.isPresent()) {
        addItemToUserOrder(user, claimed.get().getItem());
        officeAttendanceService.save(
            user.getId(), user.getPreferedName(), user.getCity(), true, LocalDate.now());
        message = Messages.POOL_ITEM_CLAIMED.getText(claimed.get().getItem().getName());
      } else {
        message = Messages.POOL_ITEM_ALREADY_TAKEN.getText();
      }

      List<SharedOrderItem> remaining = sharedOrderItemPoolService.getAvailableEntries(user.getCity());
      if (remaining.isEmpty()) {
        sendMessage(user, message, getMessageId(callback), sender);
      } else {
        InlineKeyboardMarkup keyboard =
            KeyboardUtil.createPoolInlineKeyboard(remaining, CallbackState.POOL_CLAIM);
        KeyboardUtil.addButton(
            List.of(new UserButton("Назад", CallbackState.CANCEL.toString())), keyboard);
        sendMessageWithKeyboard(user, message, keyboard, getMessageId(callback), sender);
      }
    }
  }

  private void addItemToUserOrder(User user, Item item) {
    Order order;
    if (isOrderExist(user)) {
      order = orderService.findById(user.getId());
    } else {
      order = new Order();
      order.setChatId(user.getChatId().toString());
      order.setUsername(user.getPreferedName());
      order.setCity(user.getCity());
      order.setStatus(Status.READY);
    }
    order.getOrderItemList().add(item);
    order.getCategoryItemList().add(item.getCategory());
    orderService.save(order);
  }

  private String getEntryId(CallbackQuery callback) {
    return callback.getData().split(":")[1];
  }
}
