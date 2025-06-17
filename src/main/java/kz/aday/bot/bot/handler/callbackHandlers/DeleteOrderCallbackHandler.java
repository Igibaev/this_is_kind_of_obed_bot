package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class DeleteOrderCallbackHandler extends AbstractHandler implements CallbackHandler {
    @Override
    public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
        if (isUserExistAndReady(callback)) {
            User user = userService.findById(getChatId(callback).toString());
            if (isOrderExist(user)) {
                Order order = orderService.findById(user.getId());
                order.setStatus(Status.DELETED);
                orderService.save(order);
                sendMessage(user, ORDER_DELETED, getMessageId(callback), sender);
            }
        }
    }

    @Override
    public boolean canHandle(CallbackQuery callback) {
        String[] data = callback.getData().split(":");
        if (data.length <= 0) {
            throw new IllegalArgumentException("There is no callback");
        }
        return CallbackState.DELETE_ORDER.toString().equals(data[0]);
    }

    private static final String ORDER_DELETED = "Ваш заказ удален./return";

}
