package kz.aday.bot.bot.handler.commandHamndlers;


import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageService;
import kz.aday.bot.service.UserService;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Objects;

public abstract class CommandHandler {
    protected final UserService userService = ServiceContainer.getUserService();
    protected final MessageService messageService = ServiceContainer.getMessageService();

    public abstract boolean canHandle(String command);
    public abstract void handle(Update update, AbsSender sender) throws Exception;

    public Long getChatId(Update update) {
        return update.getMessage().getChatId();
    }

    public boolean isUserExist(Update update) {
        return userService.isUserExist(getChatId(update));
    }

    public void sendMessage(String message, AbsSender sender) {

    }
}
