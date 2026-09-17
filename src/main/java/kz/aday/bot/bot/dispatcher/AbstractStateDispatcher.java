/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import java.util.Set;
import kz.aday.bot.bot.handler.stateHandlers.State;
import kz.aday.bot.bot.handler.stateHandlers.StateHandler;
import kz.aday.bot.model.User;
import kz.aday.bot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public abstract class AbstractStateDispatcher extends AbstractDispatcher<StateHandler> {
  protected static final String INVALID_STATE_INPUT_MESSAGE = "Некорректный state или текст команды";

  protected final UserService userService;

  protected AbstractStateDispatcher(Set<StateHandler> handlers, UserService userService) {
    super(handlers);
    this.userService = userService;
  }

  public void dispatch(Update update, AbsSender sender) throws Exception {
    log.info(
        "Dispatching state for update, chatId: {}",
        update != null && update.getMessage() != null
            ? update.getMessage().getChatId()
            : "unknown");
    validateInput(update);

    if (userService.existsById(update.getMessage().getChatId().toString())) {
      User user = userService.findById(update.getMessage().getChatId().toString());
      if (user.getState() != null && user.getState() != State.NONE) {
        handle(user.getState().getDisplayName(), update, sender);
        return;
      }
    }
    handle(update, sender);
  }

  protected abstract void validateInput(Update update);

  private void handle(Update update, AbsSender sender) throws Exception {
    handle(null, update, sender);
  }

  private void handle(String state, Update update, AbsSender sender) throws Exception {
    if (state == null || state.isBlank()) {
      state = update.getMessage().getText();
    }

    log.debug("Processing state: [{}]", state);

    String finalState = state;
    boolean matched =
        tryDispatch(state, StateHandler::canHandle, handler -> handler.handle(update, sender));
    if (!matched) {
      log.warn("Unknown state: [{}]", finalState);
      throw unmatchedHandlerException(finalState);
    }
    log.info("State handled successfully: [{}]", finalState);
  }
}
