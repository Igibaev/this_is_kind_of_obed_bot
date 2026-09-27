/* (C) 2024 Igibaev */
package kz.aday.bot;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.function.IntConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

class StartupGuardTest {
  private IntConsumer exitHandler;
  private StartupGuard startupGuard;

  @BeforeEach
  void setUp() {
    exitHandler = mock(IntConsumer.class);
    startupGuard = new StartupGuard(exitHandler);
  }

  @Test
  void runsActionAndDoesNotExitWhenStartupSucceeds() {
    boolean[] actionExecuted = {false};

    startupGuard.run(() -> actionExecuted[0] = true);

    assertTrue(actionExecuted[0]);
    verify(exitHandler, never()).accept(anyInt());
  }

  @Test
  void exitsWithFailureCodeWhenBotRegistrationFails() {
    startupGuard.run(
        () -> {
          throw new TelegramApiException("Error removing old webhook");
        });

    verify(exitHandler).accept(StartupGuard.STARTUP_FAILURE_EXIT_CODE);
    verifyNoMoreInteractions(exitHandler);
  }

  @Test
  void exitsWithFailureCodeWhenStartupThrowsRuntimeException() {
    startupGuard.run(
        () -> {
          throw new IllegalStateException("Database is unavailable");
        });

    verify(exitHandler).accept(StartupGuard.STARTUP_FAILURE_EXIT_CODE);
    verifyNoMoreInteractions(exitHandler);
  }
}
