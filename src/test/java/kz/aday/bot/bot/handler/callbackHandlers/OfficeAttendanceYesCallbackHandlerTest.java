/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
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

class OfficeAttendanceYesCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OfficeAttendanceService officeAttendanceService;
  private MessageSender messageSender;
  private AbsSender sender;
  private OfficeAttendanceYesCallbackHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    officeAttendanceService = services.getOfficeAttendanceService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new OfficeAttendanceYesCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsAttendanceYes() {
    CallbackQuery callback = callbackQuery(CallbackState.ATTENDANCE_YES.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_savesTodayAttendanceAndThanks_whenTodaySuffix() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    CallbackQuery callback = callbackQuery(CallbackState.ATTENDANCE_YES.name() + ":TODAY");

    handler.handle(callback, sender);

    verify(officeAttendanceService)
        .save(user.getId(), user.getPreferedName(), user.getCity(), true, LocalDate.now());
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.THANKS_WILL_COME_TODAY.getText(), messageCaptor.getValue().getText());
  }

  @Test
  void handle_savesTomorrowAttendanceAndThanks_whenTomorrowSuffix() throws Exception {
    User user = readyUser();
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    CallbackQuery callback = callbackQuery(CallbackState.ATTENDANCE_YES.name() + ":TOMORROW");

    handler.handle(callback, sender);

    verify(officeAttendanceService)
        .save(
            user.getId(),
            user.getPreferedName(),
            user.getCity(),
            true,
            LocalDate.now().plusDays(1));
    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertEquals(Messages.THANKS_WILL_COME_TOMORROW.getText(), messageCaptor.getValue().getText());
  }
}
