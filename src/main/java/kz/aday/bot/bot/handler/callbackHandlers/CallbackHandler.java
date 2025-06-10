package kz.aday.bot.bot.handler.callbackHandlers;

import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.service.MessageService;
import kz.aday.bot.service.UserService;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public abstract class CallbackHandler {
    private final UserService userService = ServiceContainer.getUserService();
    private final MessageService messageService = ServiceContainer.getMessageService();

    public abstract void handle(CallbackQuery callback, AbsSender sender) throws Exception;
    public abstract boolean canHandle(CallbackQuery callback);


    public Long getChatId(CallbackQuery update) {
        return update.getMessage().getChatId();
    }

    public boolean isUserExist(CallbackQuery update) {
        return userService.isUserExist(getChatId(update));
    }
}
