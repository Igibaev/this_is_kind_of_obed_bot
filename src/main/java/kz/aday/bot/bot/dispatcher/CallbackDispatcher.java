/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import java.util.HashSet;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackHandler;
import kz.aday.bot.configuration.FeatureToggles;
import kz.aday.bot.configuration.ServiceContainer;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public class CallbackDispatcher extends AbstractDispatcher<CallbackHandler> {

  private final FeatureToggles featureToggles;

  public CallbackDispatcher() {
    this(ServiceContainer.getFeatureToggles());
  }

  public CallbackDispatcher(FeatureToggles featureToggles) {
    super(new HashSet<>());
    this.featureToggles = featureToggles;
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
    if (featureToggles.isCallbackDisabled(callback.getData())) {
      log.warn("Disabled callback: [{}]", callback.getData());
      throw unmatchedHandlerException(callback.getMessage().getText());
    }

    boolean matched =
        tryDispatch(
            callback,
            CallbackHandler::canHandle,
            handler -> {
              try {
                handler.handle(callback, sender);
              } catch (Exception ex) {
                log.error("Error handling callback: [{}]", callback.getData(), ex);
                throw ex;
              }
            });
    if (!matched) {
      log.warn("Unknown callback: [{}]", callback.getData());
      throw unmatchedHandlerException(callback.getMessage().getText());
    }
    log.info("Callback handled successfully: [{}]", callback.getData());
  }
}
