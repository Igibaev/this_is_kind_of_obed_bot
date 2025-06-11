package kz.aday.bot.bot.handler.stateHandlers;

import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class StateHandler {
    protected final UserService userService = ServiceContainer.getUserService();
    protected final MessageSender messageService = ServiceContainer.getMessageService();
    protected final MenuService menuService = ServiceContainer.getMenuService();
    protected final OrderService orderService = ServiceContainer.getOrderService();

    public boolean canHandle(String state, User user) {
        if (user == null) {
            return canHandle(state);
        }
        return canHandle(user.getState().toString());
    }

    public abstract boolean canHandle(String state);

    public abstract void handle(Update update, AbsSender sender) throws Exception;


    public Long getChatId(Update update) {
        return update.getMessage().getChatId();
    }

    boolean isUserExist(Update update) {
        return userService.findByIdOptional(getChatId(update).toString())
                .filter(user -> user.getCity() != null)
                .isPresent();
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

    boolean isMenuPending(City city) {
        if (isMenuExist(city)) {
            Menu menu = menuService.findById(city.toString());
            return menu.getStatus() == Status.PENDING;
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

    boolean isOrderPending(User user) {
        if (isOrderExist(user)) {
            Order order = orderService.findById(user.getId());
            return order.getStatus() == Status.PENDING;
        }
        return false;
    }

    public void sendMessage(User user, String text, ReplyKeyboard keyboard, AbsSender sender) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(text);
        message.setReplyMarkup(keyboard);
        message.enableMarkdown(true);
        Message sendedMessage = messageService.sendMessage(message, sender);
        user.setLastMessageId(sendedMessage.getMessageId());
        userService.save(user);
    }

    public void sendMessage(User user, String text, AbsSender sender) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(text);
        message.enableMarkdown(true);
        Message sendedMessage = messageService.sendMessage(message, sender);
        user.setLastMessageId(sendedMessage.getMessageId());
        userService.save(user);
    }
}
