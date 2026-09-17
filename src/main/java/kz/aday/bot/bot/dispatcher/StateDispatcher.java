/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import java.util.HashSet;
import kz.aday.bot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
public class StateDispatcher extends AbstractStateDispatcher {

  public StateDispatcher(UserService userService) {
    super(new HashSet<>(), userService);
  }

  @Override
  protected void validateInput(Update update) {
    if (update == null
        || update.getMessage() == null
        || update.getMessage().getText() == null
        || update.getMessage().getChatId() == null) {
      log.error("Invalid update or state text");
      throw new IllegalArgumentException(INVALID_STATE_INPUT_MESSAGE);
    }
  }
}
