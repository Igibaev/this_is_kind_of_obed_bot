/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers.navigation;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.model.City;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.UserService;
import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class BackToMenuStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private MessageSender messageSender;
  private MenuService menuService;
  private AbsSender sender;
  private BackToMenuStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    messageSender = services.getMessageService();
    menuService = services.getMenuService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender), eq(true))).thenReturn(sentMessage);

    handler = new BackToMenuStateHandler();
  }

  @Test
  void canHandle_givenBackButtonText_whenCalled_thenReturnsTrue() {
    assertTrue(handler.canHandle(State.BACK_TO_MENU.getDisplayName()));
  }

  @Test
  void handle_resetsStateAndSendsTopLevelMenu_whenUserExists() throws Exception {
    // given
    User user = readyUser();
    user.setState(State.PROFILE_MENU);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(userService.findById(CHAT_ID_STRING)).thenReturn(user);
    when(menuService.findByIdOptional(City.ALMATA.toString())).thenReturn(Optional.empty());
    Update update = update();
    // when
    handler.handle(update, sender);
    // then
    assertEquals(State.NONE, user.getState());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender), eq(true));
    assertTrue(messageCaptor.getValue().getReplyMarkup() != null);
  }
}
