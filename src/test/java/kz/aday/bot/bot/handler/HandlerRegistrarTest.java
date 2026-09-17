/* (C) 2024 Igibaev */
package kz.aday.bot.bot.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import kz.aday.bot.bot.TelegramFoodBot;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.bot.handler.commandHandlers.CommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.ContentAwareStateHandler;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.service.MenuService;
import kz.aday.bot.service.MessageSender;
import kz.aday.bot.service.OfficeAttendanceService;
import kz.aday.bot.service.OrderService;
import kz.aday.bot.service.SharedOrderItemPoolService;
import kz.aday.bot.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

class HandlerRegistrarTest {

  private MockedStatic<ServiceContainer> serviceContainer;

  @BeforeEach
  void setUp() {
    serviceContainer = mockStatic(ServiceContainer.class);
    serviceContainer.when(ServiceContainer::getUserService).thenReturn(mock(UserService.class));
    serviceContainer
        .when(ServiceContainer::getMessageService)
        .thenReturn(mock(MessageSender.class));
    serviceContainer.when(ServiceContainer::getMenuService).thenReturn(mock(MenuService.class));
    serviceContainer.when(ServiceContainer::getOrderService).thenReturn(mock(OrderService.class));
    serviceContainer
        .when(ServiceContainer::getPoolService)
        .thenReturn(mock(SharedOrderItemPoolService.class));
    serviceContainer
        .when(ServiceContainer::getOfficeAttendanceService)
        .thenReturn(mock(OfficeAttendanceService.class));
  }

  @AfterEach
  void tearDown() {
    serviceContainer.close();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("registerCases")
  void register_givenHandlerType_whenCalled_thenDispatchesToMatchingBotMethod(
      String caseName,
      Supplier<AbstractHandler> handlerFactory,
      boolean expectedResult,
      BiConsumer<TelegramFoodBot, AbstractHandler> verification) {
    // given
    AbstractHandler testHandler = handlerFactory.get();
    TelegramFoodBot bot = mock(TelegramFoodBot.class);
    // when
    boolean actual = testHandler.register(bot);
    // then
    assertEquals(expectedResult, actual);
    verification.accept(bot, testHandler);
  }

  static Stream<Arguments> registerCases() {
    return Stream.of(
        Arguments.of(
            "state handler",
            (Supplier<AbstractHandler>) StateHandlerDouble::new,
            true,
            (BiConsumer<TelegramFoodBot, AbstractHandler>)
                (bot, h) -> verify(bot).addStateHandler((StateHandler) h)),
        Arguments.of(
            "command handler",
            (Supplier<AbstractHandler>) CommandHandlerDouble::new,
            true,
            (BiConsumer<TelegramFoodBot, AbstractHandler>)
                (bot, h) -> verify(bot).addCommandHandler((CommandHandler) h)),
        Arguments.of(
            "callback handler",
            (Supplier<AbstractHandler>) CallbackHandlerDouble::new,
            true,
            (BiConsumer<TelegramFoodBot, AbstractHandler>)
                (bot, h) -> verify(bot).addCallbackHandler((CallbackHandler) h)),
        Arguments.of(
            "unknown handler",
            (Supplier<AbstractHandler>) UnknownHandlerDouble::new,
            false,
            (BiConsumer<TelegramFoodBot, AbstractHandler>) (bot, h) -> verifyNoInteractions(bot)));
  }

  @Test
  void register_givenContentAwareStateHandler_whenCalled_thenRegistersOnBothDispatchers() {
    // given
    ContentAwareStateHandlerDouble handler = new ContentAwareStateHandlerDouble();
    TelegramFoodBot bot = mock(TelegramFoodBot.class);
    // when
    boolean actual = handler.register(bot);
    // then
    assertEquals(true, actual);
    verify(bot).addStateHandler(handler);
    verify(bot).addStateWithContentHandler(handler);
  }

  private static class StateHandlerDouble extends AbstractHandler implements StateHandler {
    @Override
    public boolean canHandle(String state) {
      return false;
    }

    @Override
    public void handle(Update update, AbsSender sender) {}
  }

  private static class ContentAwareStateHandlerDouble extends AbstractHandler
      implements ContentAwareStateHandler {
    @Override
    public boolean canHandle(String state) {
      return false;
    }

    @Override
    public void handle(Update update, AbsSender sender) {}
  }

  private static class CommandHandlerDouble extends AbstractHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
      return false;
    }

    @Override
    public void handle(Update update, AbsSender sender) {}
  }

  private static class CallbackHandlerDouble extends AbstractHandler implements CallbackHandler {
    @Override
    public void handle(CallbackQuery callback, AbsSender sender) {}

    @Override
    public boolean canHandle(CallbackQuery callback) {
      return false;
    }
  }

  private static class UnknownHandlerDouble extends AbstractHandler {}
}
