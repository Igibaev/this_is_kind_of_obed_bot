package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.bot.handler.AbstractHandler;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SubmitTempOrderCallbackHandler extends AbstractHandler implements CallbackHandler {
    @Override
    public void handle(CallbackQuery callback, AbsSender sender) throws Exception {
        if (isUserExistAndReady(callback)) {
            User user = userService.findById(getChatId(callback).toString());
            Menu menu = menuService.findById(user.getCity().toString());
            Order order = orderService.findById(getOrderId(callback));
            if (menu.isDeadlinePassed()) {
                sendMessage(user, MENU_DEADLINE_IS_PASSED, getMessageId(callback), sender);
            }
            order.setStatus(Status.READY);
            orderService.save(order);
            sendMessage(user, String.format(ORDER_SENDED, order.getOrderItemList()), getMessageId(callback), sender);
        }
    }

    @Override
    public boolean canHandle(CallbackQuery callback) {
        String[] data = callback.getData().split(":");
        if (data.length <= 0) {
            throw new IllegalArgumentException("There is no callback");
        }
        return CallbackState.SUBMIT_TEMP_ORDER.toString().equals(data[0]);
    }

    private static final String MENU_DEADLINE_IS_PASSED = "Дедлайн уже прошел.";

    private static final String ORDER_SENDED = "Ваш заказ улетел.%s./return";

    private String getOrderId(CallbackQuery callback) {
        return callback.getData().split(":")[1];
    }

}
