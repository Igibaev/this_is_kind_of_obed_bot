/* (C) 2024 Igibaev */
package kz.aday.bot;

import java.util.function.IntConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class StartupGuard {
  static final int STARTUP_FAILURE_EXIT_CODE = 1;

  private final IntConsumer exitHandler;

  public void run(StartupAction action) {
    try {
      action.run();
    } catch (Exception e) {
      log.error("Application startup failed, exiting with code {}", STARTUP_FAILURE_EXIT_CODE, e);
      exitHandler.accept(STARTUP_FAILURE_EXIT_CODE);
    }
  }

  @FunctionalInterface
  public interface StartupAction {
    void run() throws Exception;
  }
}
