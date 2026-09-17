/* (C) 2024 Igibaev */
package kz.aday.bot.testsupport;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;

public final class ServiceContainerMockExtension implements BeforeEachCallback, AfterEachCallback {

  private final UserService userService = mock(UserService.class);
  private final MessageSender messageService = mock(MessageSender.class);
  private final OrderService orderService = mock(OrderService.class);
  private final MenuService menuService = mock(MenuService.class);
  private final OfficeAttendanceService officeAttendanceService =
      mock(OfficeAttendanceService.class);
  private final SharedOrderItemPoolService poolService = mock(SharedOrderItemPoolService.class);

  private MockedStatic<ServiceContainer> mockedServiceContainer;

  @Override
  public void beforeEach(ExtensionContext context) {
    mockedServiceContainer = mockStatic(ServiceContainer.class);
    mockedServiceContainer.when(ServiceContainer::getUserService).thenReturn(userService);
    mockedServiceContainer.when(ServiceContainer::getMessageService).thenReturn(messageService);
    mockedServiceContainer.when(ServiceContainer::getOrderService).thenReturn(orderService);
    mockedServiceContainer.when(ServiceContainer::getMenuService).thenReturn(menuService);
    mockedServiceContainer
        .when(ServiceContainer::getOfficeAttendanceService)
        .thenReturn(officeAttendanceService);
    mockedServiceContainer.when(ServiceContainer::getPoolService).thenReturn(poolService);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    mockedServiceContainer.close();
  }

  public UserService getUserService() {
    return userService;
  }

  public MessageSender getMessageService() {
    return messageService;
  }

  public OrderService getOrderService() {
    return orderService;
  }

  public MenuService getMenuService() {
    return menuService;
  }

  public OfficeAttendanceService getOfficeAttendanceService() {
    return officeAttendanceService;
  }

  public SharedOrderItemPoolService getPoolService() {
    return poolService;
  }
}
