/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.User;
import kz.aday.bot.util.Messages;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class DeleteOrderCallbackHandler extends AbstractHandler implements CallbackHandler {
  @Override
  public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
    Optional<User> optionalUser = findReadyUserByChatId(callback);
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      if (isOrderExist(user)) {
        List<Item> shared = releaseOrderToSharedOrderItemPool(user);
        String text = Messages.ORDER_DELETED_RETURN.getText();
        if (!shared.isEmpty()) {
          text += "\n\n" + Messages.POOL_ORDER_SHARED.getText(joinItemNames(shared));
        }
        sendMessage(user, text, getMessageId(callback), sender);
      }
    }
  }

  @Override
  public boolean canHandle(CallbackQuery callback) {
    return canHandle(callback, CallbackState.DELETE_ORDER);
  }
}
