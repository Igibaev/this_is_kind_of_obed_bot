/* (C) 2024 Igibaev */
package kz.aday.bot.configuration;

import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import lombok.Getter;

@Getter
public class ServiceContainer {
  private static final MessageSender messageService = new MessageSender();
  private static final SharedOrderItemPoolService SHARED_ORDER_ITEM_POOL_SERVICE =
      new SharedOrderItemPoolService();

  private ServiceContainer() {}

  public static UserService getUserService() {
    return UserServiceHolder.INSTANCE;
  }

  public static MessageSender getMessageService() {
    return messageService;
  }

  public static OrderService getOrderService() {
    return OrderServiceHolder.INSTANCE;
  }

  public static MenuService getMenuService() {
    return MenuServiceHolder.INSTANCE;
  }

  public static OfficeAttendanceService getOfficeAttendanceService() {
    return OfficeAttendanceServiceHolder.INSTANCE;
  }

  public static SharedOrderItemPoolService getPoolService() {
    return SHARED_ORDER_ITEM_POOL_SERVICE;
  }

  private static final class UserServiceHolder {
    private static final UserService INSTANCE = new UserService();
  }

  private static final class OrderServiceHolder {
    private static final OrderService INSTANCE = new OrderService();
  }

  private static final class MenuServiceHolder {
    private static final MenuService INSTANCE = new MenuService();
  }

  private static final class OfficeAttendanceServiceHolder {
    private static final OfficeAttendanceService INSTANCE = new OfficeAttendanceService();
  }
}
