/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler.callbackHandlers;

import static kz.aday.bot.testsupport.TestFixtures.callbackQuery;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import kz.aday.bot.testsupport.ServiceContainerMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

class NoneCallbackHandlerTest {

  @RegisterExtension ServiceContainerMockExtension services = new ServiceContainerMockExtension();

  private NoneCallbackHandler handler;

  @BeforeEach
  void setUp() {
    handler = new NoneCallbackHandler();
  }

  @Test
  void canHandle_returnsTrue_whenCallbackStateIsNone() {
    CallbackQuery callback = callbackQuery(CallbackState.NONE.name());
    assertTrue(handler.canHandle(callback));
  }

  @Test
  void handle_doesNothing() throws Exception {
    AbsSender sender = mock(AbsSender.class);
    CallbackQuery callback = callbackQuery(CallbackState.NONE.name());

    handler.handle(callback, sender);

    verifyNoInteractions(sender);
  }
}
