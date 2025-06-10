package kz.aday.bot.configuration;


import kz.aday.bot.service.MessageService;
import kz.aday.bot.service.UserService;


public class ServiceContainer {
    private static final UserService userService = new UserService();
    private static final MessageService messageService = new MessageService();

    private ServiceContainer() {
    }

    public static UserService getUserService() {
        return userService;
    }

    public static MessageService getMessageService() {
        return messageService;

    }
}
