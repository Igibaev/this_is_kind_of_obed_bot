package kz.aday.bot.configuration;


import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.ReportService;
import kz.aday.bot.service.UserService;


public class ServiceContainer {
    private static final UserService userService = new UserService();
    private static final MessageSender messageService = new MessageSender();
    private static final ReportService reportService = new ReportService();
    private static final OrderService orderService = new OrderService();
    private static final MenuService menuService = new MenuService();

    private ServiceContainer() {
    }

    public static UserService getUserService() {
        return userService;
    }

    public static MessageSender getMessageService() {
        return messageService;
    }

    public static OrderService getOrderService() {
        return orderService;
    }

    public static MenuService getMenuService() {
        return menuService;
    }

    public static ReportService getReportService() {
        return reportService;
    }


}
