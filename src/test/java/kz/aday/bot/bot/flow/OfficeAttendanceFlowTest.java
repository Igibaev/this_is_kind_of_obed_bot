/* (C) 2024 Igibaev */
package kz.aday.bot.bot.flow;

import static kz.aday.bot.testsupport.TestFixtures.callbackQueryWithChatId;
import static kz.aday.bot.testsupport.TestFixtures.updateWithChatId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.configuration.PersistenceConfig;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.City;
import kz.aday.bot.model.OfficeAttendance;
import kz.aday.bot.model.Status;
import kz.aday.bot.model.User;
import kz.aday.bot.repository.JdbcOfficeAttendanceRepository;
import kz.aday.bot.testsupport.AbstractDbPersistenceTest;
import kz.aday.bot.testsupport.RealDispatchers;
import kz.aday.bot.util.Messages;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class OfficeAttendanceFlowTest extends AbstractDbPersistenceTest {

  private static final String NO_DATA_MESSAGE = "Нет данных о посещениях.";
  private static final JdbcOfficeAttendanceRepository ATTENDANCE_REPOSITORY =
      new JdbcOfficeAttendanceRepository(PersistenceConfig.getDataSource());

  @Test
  void todayYes_savesForToday_andShowsInBothOverallAndMyStats() throws Exception {
    Long userChatId = 953000001L;
    Long adminChatId = 953000002L;
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    seedReadyUser(userChatId, City.ALMATA, User.Role.USER, "Аружан");
    seedReadyUser(adminChatId, City.ALMATA, User.Role.ADMIN, "Admin");

    setOfficeAttendance(dispatchers, sender, userChatId);
    chooseDay(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_DAY_TODAY);
    answer(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_YES, "TODAY");

    LocalDate today = LocalDate.now();
    OfficeAttendance saved = ATTENDANCE_REPOSITORY.getById(userChatId + "_" + today, today);
    assertEquals(City.ALMATA, saved.getCity());
    assertEquals(true, saved.getWillCome());
    assertEquals(today.toString(), saved.getDate());

    Update overallStats =
        updateWithChatId(adminChatId, State.GET_ATTENDANCE_STATS.getDisplayName());
    dispatchers.stateDispatcher.dispatch(overallStats, sender);
    assertTrue(
        messageSentTo(sender, adminChatId).contains("Аружан: 1"),
        "Expected admin overall stats to include Аружан's attendance");

    Update myStats = updateWithChatId(userChatId, State.GET_MY_ATTENDANCE_STATS.getDisplayName());
    dispatchers.stateDispatcher.dispatch(myStats, sender);
    assertTrue(
        messageSentTo(sender, userChatId).contains("Аружан: 1"),
        "Expected user's own stats to include their attendance");
  }

  @Test
  void tomorrowYes_savesForTomorrow_butExcludedFromTodayOverallStats() throws Exception {
    Long userChatId = 953000003L;
    Long adminChatId = 953000004L;
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    seedReadyUser(userChatId, City.ASTANA, User.Role.USER, "Тамирлан");
    seedReadyUser(adminChatId, City.ASTANA, User.Role.ADMIN, "Admin");

    setOfficeAttendance(dispatchers, sender, userChatId);
    chooseDay(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_DAY_TOMORROW);
    answer(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_YES, "TOMORROW");

    LocalDate tomorrow = LocalDate.now().plusDays(1);
    OfficeAttendance saved = ATTENDANCE_REPOSITORY.getById(userChatId + "_" + tomorrow, tomorrow);
    assertEquals(true, saved.getWillCome());
    assertEquals(tomorrow.toString(), saved.getDate());

    Update overallStats =
        updateWithChatId(adminChatId, State.GET_ATTENDANCE_STATS.getDisplayName());
    dispatchers.stateDispatcher.dispatch(overallStats, sender);
    assertTrue(
        messageSentTo(sender, adminChatId)
            .startsWith(Messages.REPORT_OVERALL_ATTENDANCE.getText() + NO_DATA_MESSAGE),
        "Attendance marked for a future date should not appear in today's overall stats");
  }

  @Test
  void todayNo_savesWillComeFalse_excludedFromMyStats() throws Exception {
    Long userChatId = 953000005L;
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    seedReadyUser(userChatId, City.KARAGANDA, User.Role.USER, "Данияр");

    setOfficeAttendance(dispatchers, sender, userChatId);
    chooseDay(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_DAY_TODAY);
    answer(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_NO, "TODAY");

    LocalDate today = LocalDate.now();
    OfficeAttendance saved = ATTENDANCE_REPOSITORY.getById(userChatId + "_" + today, today);
    assertEquals(false, saved.getWillCome());

    Update myStats = updateWithChatId(userChatId, State.GET_MY_ATTENDANCE_STATS.getDisplayName());
    dispatchers.stateDispatcher.dispatch(myStats, sender);
    assertTrue(
        messageSentTo(sender, userChatId)
            .startsWith(Messages.REPORT_MY_OVERALL_ATTENDANCE.getText() + NO_DATA_MESSAGE));
  }

  @Test
  void changingAnswerSameDay_latestDecisionWins() throws Exception {
    Long userChatId = 953000006L;
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();
    seedReadyUser(userChatId, City.ALMATA, User.Role.USER, "Ерлан");

    setOfficeAttendance(dispatchers, sender, userChatId);
    chooseDay(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_DAY_TODAY);
    answer(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_YES, "TODAY");

    setOfficeAttendance(dispatchers, sender, userChatId);
    chooseDay(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_DAY_TODAY);
    answer(dispatchers, sender, userChatId, CallbackState.ATTENDANCE_NO, "TODAY");

    LocalDate today = LocalDate.now();
    OfficeAttendance saved = ATTENDANCE_REPOSITORY.getById(userChatId + "_" + today, today);
    assertEquals(false, saved.getWillCome(), "The later answer should overwrite the earlier one");
    long rowsForChat =
        ATTENDANCE_REPOSITORY.getAll(today).stream()
            .filter(a -> userChatId.toString().equals(a.getChatId()))
            .count();
    assertEquals(1, rowsForChat, "Changing the answer must overwrite, not duplicate, the row");
  }

  @Test
  void unregisteredUser_callbackIsNoop_noRecordCreated() throws Exception {
    Long unregisteredChatId = 953000007L;
    AbsSender sender = mockSender();
    RealDispatchers dispatchers = new RealDispatchers();

    CallbackQuery answer =
        callbackQueryWithChatId(unregisteredChatId, CallbackState.ATTENDANCE_YES.name() + ":TODAY");
    assertDoesNotThrow(() -> dispatchers.callbackDispatcher.dispatch(answer, sender));

    LocalDate today = LocalDate.now();
    assertNull(ATTENDANCE_REPOSITORY.getById(unregisteredChatId + "_" + today, today));
  }

  private static void setOfficeAttendance(
      RealDispatchers dispatchers, AbsSender sender, Long chatId) throws Exception {
    Update start = updateWithChatId(chatId, State.SET_OFFICE_ATTENDANCE.getDisplayName());
    dispatchers.stateDispatcher.dispatch(start, sender);
  }

  private static void chooseDay(
      RealDispatchers dispatchers, AbsSender sender, Long chatId, CallbackState day)
      throws Exception {
    CallbackQuery dayChoice = callbackQueryWithChatId(chatId, day.name());
    dispatchers.callbackDispatcher.dispatch(dayChoice, sender);
  }

  private static void answer(
      RealDispatchers dispatchers,
      AbsSender sender,
      Long chatId,
      CallbackState yesOrNo,
      String daySuffix)
      throws Exception {
    CallbackQuery response = callbackQueryWithChatId(chatId, yesOrNo.name() + ":" + daySuffix);
    dispatchers.callbackDispatcher.dispatch(response, sender);
  }

  private static String messageSentTo(AbsSender sender, Long chatId) throws Exception {
    ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
    verify(sender, atLeastOnce()).execute(captor.capture());
    return captor.getAllValues().stream()
        .filter(message -> chatId.toString().equals(message.getChatId()))
        .reduce((first, second) -> second)
        .map(SendMessage::getText)
        .orElseThrow(() -> new AssertionError("No message was sent to chatId " + chatId));
  }

  private static void seedReadyUser(Long chatId, City city, User.Role role, String preferedName) {
    User user =
        User.builder()
            .chatId(chatId)
            .preferedName(preferedName)
            .city(city)
            .role(role)
            .status(Status.READY)
            .state(State.NONE)
            .build();
    ServiceContainer.getUserService().save(user);
  }

  private static AbsSender mockSender() throws Exception {
    AbsSender sender = mock(AbsSender.class);
    Message sentMessage = mock(Message.class);
    when(sentMessage.getMessageId()).thenReturn(999);
    when(sender.execute(any(SendMessage.class))).thenReturn(sentMessage);
    return sender;
  }
}
