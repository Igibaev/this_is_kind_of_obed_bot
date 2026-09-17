/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import java.util.Set;
import java.util.function.BiPredicate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDispatcher<T> {
  protected static final String UNMATCHED_HANDLER_MESSAGE =
      "Неизвестная команда [%s]. Вернитесь в меню /return";

  protected final Set<T> handlers;

  protected AbstractDispatcher(Set<T> handlers) {
    this.handlers = handlers;
  }

  public void addHandler(T handler) {
    handlers.add(handler);
  }

  protected <K> boolean tryDispatch(K key, BiPredicate<T, K> canHandle, HandlerAction<T> action)
      throws Exception {
    for (T handler : handlers) {
      if (canHandle.test(handler, key)) {
        action.run(handler);
        return true;
      }
    }
    return false;
  }

  protected RuntimeException unmatchedHandlerException(Object errorKey) {
    return new RuntimeException(String.format(UNMATCHED_HANDLER_MESSAGE, errorKey));
  }

  @FunctionalInterface
  protected interface HandlerAction<T> {
    void run(T handler) throws Exception;
  }
}
