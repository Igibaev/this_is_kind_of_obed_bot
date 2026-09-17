/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.commandHandlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class GetLogsCommandHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private GetLogsCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GetLogsCommandHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCommandStartsWithGetLogs() {
    assertTrue(handler.canHandle("/getlogs"));
    assertTrue(handler.canHandle("/getlogs 100"));
  }

  @Test
  void canHandle_returnsFalse_whenCommandIsUnrelated() {
    assertFalse(handler.canHandle("/menu"));
  }
}
