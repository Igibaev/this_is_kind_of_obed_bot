/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.stateHandlers;

import static kz.aday.bot.testsupport.TestFixtures.CHAT_ID_STRING;
import static kz.aday.bot.testsupport.TestFixtures.readyUser;
import static kz.aday.bot.testsupport.TestFixtures.update;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.aday.bot.model.City;
import kz.aday.bot.model.User;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
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

class GetMyAttendanceStatsStateHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private UserService userService;
  private OfficeAttendanceService officeAttendanceService;
  private MessageSender messageSender;
  private AbsSender sender;
  private GetMyAttendanceStatsStateHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    userService = services.getUserService();
    officeAttendanceService = services.getOfficeAttendanceService();
    messageSender = services.getMessageService();
    sender = mock(AbsSender.class);

    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(messageSender.sendMessage(any(), eq(sender))).thenReturn(sentMessage);

    handler = new GetMyAttendanceStatsStateHandler();
  }

  @Test
  void canHandle_returnsTrue_whenStateIsGetMyAttendanceStats() {
    assertTrue(handler.canHandle(State.GET_MY_ATTENDANCE_STATS.getDisplayName()));
  }

  @Test
  void handle_sendsPersonalReport() throws Exception {
    User user = readyUser(City.ALMATA);
    when(userService.findByIdOptional(CHAT_ID_STRING)).thenReturn(Optional.of(user));
    when(officeAttendanceService.getOverallAttendanceStatsForUser(City.ALMATA, CHAT_ID_STRING))
        .thenReturn("3 visits");
    Update update = update();

    handler.handle(update, sender);

    ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
    verify(messageSender).sendMessage(messageCaptor.capture(), eq(sender));
    assertTrue(messageCaptor.getValue().getText().contains("3 visits"));
  }
}
