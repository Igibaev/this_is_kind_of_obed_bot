package kz.aday.bot.bot.handler.commandHamndlers;


import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Menu;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.util.KeyboardUtil;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

import static kz.aday.bot.model.User.Role.ADMIN;

public abstract class CommandHandler {
    protected final UserService userService = ServiceContainer.getUserService();
    protected final MessageSender messageService = ServiceContainer.getMessageService();
    protected final MenuService menuService = ServiceContainer.getMenuService();
    protected final OrderService orderService = ServiceContainer.getOrderService();

    public abstract boolean canHandle(String command);
    public abstract void handle(Update update, AbsSender sender) throws Exception;

    public Long getChatId(Update update) {
        return update.getMessage().getChatId();
    }

    public Integer getMessageId(Update update) {
        return update.getMessage().getMessageId();
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

    public void sendMessage(User user, String text, ReplyKeyboard keyboard, Integer lastUserSendedMessageId, AbsSender sender) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(text);
        message.setReplyMarkup(keyboard);
        message.enableMarkdown(true);
        Message sendedMessage = messageService.sendMessage(message, sender);
        messageService.deleteMessage(user.getChatId(), List.of(user.getLastMessageId(), lastUserSendedMessageId), sender);

        user.setLastMessageId(sendedMessage.getMessageId());
        userService.save(user);
    }

    public void sendMessage(User user, String text, Integer lastUserSendedMessageId, AbsSender sender) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(text);
        message.enableMarkdown(true);
        Message sendedMessage = messageService.sendMessage(message, sender);
        messageService.deleteMessage(user.getChatId(), List.of(user.getLastMessageId(), lastUserSendedMessageId), sender);

        user.setLastMessageId(sendedMessage.getMessageId());
        userService.save(user);
    }


    ReplyKeyboard getUserMenuKeyboard(User user) {
        boolean isMenuExist = isMenuExist(user.getCity());
        boolean isMenuReady = isMenuReady(user.getCity());
        boolean isOrderExist = isOrderExist(user);
        boolean isOrderReady = isOrderReady(user);

        List<String> userMenuItems = new ArrayList<>();
        userMenuItems.add(CallbackState.PROFILE.getDisplayName());
        userMenuItems.add(CallbackState.EDIT_USERNAME.getDisplayName());
        userMenuItems.add(CallbackState.TEMP_ORDER_FOR_USER.getDisplayName());

        if (isMenuExist && isMenuReady) {
            if (isOrderExist) {
                if (isOrderReady) {
                    userMenuItems.add(CallbackState.DELETE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.GET_ORDER.getDisplayName());
                } else {
                    userMenuItems.add(CallbackState.SUBMIT_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_ORDER.getDisplayName());
                    userMenuItems.add(CallbackState.GET_ORDER.getDisplayName());
                }
            } else {
                userMenuItems.add(CallbackState.CREATE_ORDER.getDisplayName());
            }
        }

        if (user.getRole() == ADMIN) {
            userMenuItems.add(CallbackState.INPUT_MESSAGE_TO_ALL_USERS.getDisplayName());
            userMenuItems.add(CallbackState.GET_ALL_ORDERS.getDisplayName());
            if (isMenuExist) {
                if (isMenuReady) {
                    userMenuItems.add(CallbackState.CLEAR_MENU.getDisplayName());
                    userMenuItems.add(CallbackState.CHANGE_MENU.getDisplayName());
                } else {
                    userMenuItems.add(CallbackState.PUBLISH_MENU.getDisplayName());
                }
            } else {
                userMenuItems.add(CallbackState.CREATE_MENU.getDisplayName());
            }
        }
        return KeyboardUtil.createReplyKeyboard(userMenuItems);
    }

}
