/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.Category;
import kz.aday.bot.model.City;
import kz.aday.bot.model.Item;
import kz.aday.bot.model.SharedOrderItem;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class ViewSharedOrderItemPoolStateHandlerTest {

  private static final Long CHAT_ID = 1L;
  private static final String CHAT_ID_STRING = "1";
  private static final Integer MESSAGE_ID = 42;
  private static final LocalDate TARGET_DATE = City.ALMATA.getCurrentOrderDate();

  private UserService userService;
  private SharedOrderItemPoolService sharedOrderItemPoolService;
  private MessageSender messageSender;
  private AbsSender sender;
  private ViewPoolStateHandler handler;
  private MockedStatic<ServiceContainer> serviceContainer;

  @BeforeEach
  void setUp() throws Exception {
    userService = mock(UserService.class);
    sharedOrderItemPoolService = mock(SharedOrderItemPoolService.class);
    messageSender = mock(MessageSender.class);
    sender = mock(AbsSender.class);

    serviceContainer = mockStatic(ServiceContainer.class);
    serviceContainer.when(ServiceContainer::getUserService).thenReturn(userService);
    serviceContainer.when(ServiceContainer::getPoolService).thenReturn(sharedOrderItemPoolService);
    serviceContainer.when(ServiceContainer::getMessageService).thenReturn(messageSender);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new ViewPoolStateHandler();
  }

  @AfterEach
  void tearDown() {
    serviceContainer.close();
  }

  @Test
  void handle_sendsPoolEmptyMessage_whenNoAvailableEntries() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(sharedOrderItemPoolService.getAvailableEntries(City.ALMATA, TARGET_DATE))
        .thenReturn(List.of());
    Update update = updateWithChatId();
    // when
    handler.handle(update, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getText().contains("Пока никто не поделился"));
  }

  @Test
  void handle_sendsKeyboardWithAvailableEntries_whenPoolNotEmpty() throws Exception {
    // given
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    SharedOrderItem entry =
        new SharedOrderItem(
            "e1", new Item(1, "Плов", Category.FIRST), "9", "otherUser", null, null);
    when(sharedOrderItemPoolService.getAvailableEntries(City.ALMATA, TARGET_DATE))
        .thenReturn(List.of(entry));
    Update update = updateWithChatId();
    // when
    handler.handle(update, sender);
    // then
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals("Доступные позиции из общего пула:", messageCaptor.getValue().getText());
  }

  @Test
  void handle_resetsUserStateToNone_whenCalled() throws Exception {
    // given
    User user = readyUser();
    user.setState(State.VIEW_POOL);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(sharedOrderItemPoolService.getAvailableEntries(City.ALMATA, TARGET_DATE))
        .thenReturn(List.of());
    Update update = updateWithChatId();
    // when
    handler.handle(update, sender);
    // then
    assertEquals(State.NONE, user.getState());
    verify(userService, atLeastOnce()).save(user);
  }

  private static User readyUser() {
    return User.builder()
        .chatId(CHAT_ID)
        .city(City.ALMATA)
        .role(User.Role.USER)
        .status(Status.READY)
        .preferedName("me")
        .build();
  }

  private static Update updateWithChatId() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    when(update.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(CHAT_ID);
    when(message.getMessageId()).thenReturn(MESSAGE_ID);
    return update;
  }
}
