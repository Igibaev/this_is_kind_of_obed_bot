/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.adminUser;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.readyOrderWithItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Order;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

class ClearMenuCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MenuService menuService;
  private OrderService orderService;
  private MessageSender messageSender;
  private AbsSender sender;
  private ClearMenuCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    menuService = services.getMenuService();
    orderService = services.getOrderService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new ClearMenuCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsClearMenu() {
    CallbackQuery callback = callbackQuery(CallbackState.CLEAR_MENU.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_deletesMenuAndCityOrdersAndConfirms_whenAdmin() throws Exception {
    User admin = adminUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(admin));
    Order almataOrder = readyOrderWithItem(City.ALMATA, "Alice");
    almataOrder.setChatId("2");
    almataOrder.setDate(City.ALMATA.getCurrentOrderDate());
    Order otherCityOrder = readyOrderWithItem(City.KARAGANDA, "Bob");
    otherCityOrder.setChatId("3");
    otherCityOrder.setDate(City.KARAGANDA.getCurrentOrderDate());
    when(orderService.findAllOnDate(City.ALMATA.getCurrentOrderDate()))
        .thenReturn(List.of(almataOrder, otherCityOrder));
    CallbackQuery callback = callbackQuery(CallbackState.CLEAR_MENU.name());

    handler.handle(callback, sender);

    verify(menuService).deleteById(City.ALMATA.toString());
    verify(orderService).deleteByChatId(almataOrder.getChatId(), almataOrder.getStorageDate());
    verify(orderService, never())
        .deleteByChatId(otherCityOrder.getChatId(), otherCityOrder.getStorageDate());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.MENU_WAS_DELETED.getText(), messageCaptor.getValue().getText());
  }
}
