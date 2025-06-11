package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.model.UserButton;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public abstract class CallbackHandler {
    protected final UserService userService = ServiceContainer.getUserService();
    protected final MessageSender messageService = ServiceContainer.getMessageService();
    protected final MenuService menuService = ServiceContainer.getMenuService();
    protected final OrderService orderService = ServiceContainer.getOrderService();

    public abstract void handle(CallbackQuery callback, AbsSender sender) throws Exception;
    public abstract boolean canHandle(CallbackQuery callback);


    public Long getChatId(CallbackQuery update) {
        return update.getMessage().getChatId();
    }

    boolean isUserExist(CallbackQuery update) {
        return userService.findByIdOptional(getChatId(update).toString()).isPresent();
    }

    boolean isMenuExist(City city) {
        return menuService.existsById(city.toString());
    }

    boolean isMenuReady(City city) {
        if (isMenuExist(city)) {
            Menu menu = menuService.findById(city.toString());
            return menu.getStatus() == Status.READY;
        }
        return false;
    }

    boolean isOrderExist(User user) {
        return orderService.existsById(user.getId());
    }

    boolean isOrderReady(User user) {
        if (isOrderExist(user)) {
            Order order = orderService.findById(user.getId());
            return order.getStatus() == Status.READY;
        }
        return false;
    }
}
