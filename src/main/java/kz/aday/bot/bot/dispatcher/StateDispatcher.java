/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import java.util.HashSet;

import kz.aday.bot.bot.handler.commandHamndlers.CommandHandler;
import kz.aday.bot.bot.handler.commandHamndlers.FeedBackCommandHandler;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.configuration.ServiceContainer;
import kz.aday.bot.model.User;
import kz.aday.bot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class StateDispatcher extends AbstractDispatcher<StateHandler> {
  private final UserService userService = ServiceContainer.getUserService();

  public StateDispatcher() {
    super(new HashSet<>());
  }

  public void dispatch(Update update, AbsSender sender) throws Exception {
    log.info(
        "Dispatching state for update, chatId: {}",
        update != null && update.getMessage() != null
            ? update.getMessage().getChatId()
            : "unknown");
    if (update == null
        || update.getMessage() == null
        || update.getMessage().getText() == null
        || update.getMessage().getChatId() == null) {
      log.error("Invalid update or state text");
      throw new IllegalArgumentException("Некорректный state или текст команды");
    }

    if (userService.existsById(update.getMessage().getChatId().toString())) {
      User user = userService.findById(update.getMessage().getChatId().toString());
      if (user.getState() != null && user.getState() != State.NONE) {
        handle(user.getState().getDisplayName(), update, sender);
        return;
      }
    }
    handle(update, sender);
  }

  private void handle(Update update, AbsSender sender) throws Exception {
    handle(null, update, sender);
  }

  private void handle(String state, Update update, AbsSender sender) throws Exception {
    if (state == null || state.isBlank()) {
      state = update.getMessage().getText();
    }

    log.debug("Processing state: [{}]", state);

    for (StateHandler handler : handlers) {
      if (handler.canHandle(state)) {
        log.info("State [{}] handler: [{}]", state, handler);
        handler.handle(update, sender);
        log.info("State handled successfully: [{}]", state);
        return;
      }
    }

    try {
      StateHandler stateHandler = handlers.stream().filter(handler -> handler instanceof FeedBackCommandHandler)
              .findFirst()
              .orElse(null);
      if (stateHandler != null) {
        log.info("State [{}] handler: [{}]", state, stateHandler);
        stateHandler.handle(update, sender);
        log.info("State [{}] handler: [{}]", state, stateHandler);
        return;
      }
    } catch (Exception e) {
      log.error("Skip feedback state handler: [{}]", state, e);
    }

    log.warn("Unknown state: [{}]", state);
    throw new RuntimeException(
        String.format("Неизвестная команда [%s]. Вернитесь в меню /return", state));
  }
}
