/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import java.util.HashSet;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class CallbackDispatcher extends AbstractDispatcher<CallbackHandler> {

  public CallbackDispatcher() {
    super(new HashSet<>());
  }

  public void dispatch(CallbackQuery callback, AbsSender sender) throws Exception {
    log.info(
        "Dispatching callback for chatId: {}",
        callback != null && callback.getMessage() != null
            ? callback.getMessage().getChatId()
            : "unknown");
    if (callback == null
        || callback.getData() == null
        || callback.getMessage() == null
        || callback.getMessage().getChatId() == null) {
      log.error("Invalid callback or callback data");
      throw new IllegalArgumentException("Callback or callback data is null");
    }

    for (CallbackHandler handler : handlers) {
      if (handler.canHandle(callback)) {
        try {
          log.debug("Processing callback data: [{}]", callback.getData());
          handler.handle(callback, sender);
          log.info("Callback handled successfully: [{}]", callback.getData());
          return;
        } catch (Exception ex) {
          log.error("Error handling callback: [{}]", callback.getData(), ex);
          throw ex;
        }
      }
    }
    log.warn("Unknown callback: [{}]", callback.getData());
  }
}
