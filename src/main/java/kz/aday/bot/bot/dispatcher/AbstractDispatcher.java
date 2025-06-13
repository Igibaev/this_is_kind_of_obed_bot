/* (C) 2024 Igibaev */
package kz.aday.bot.bot.dispatcher;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDispatcher<T> {
  protected final Set<T> handlers;

  protected AbstractDispatcher(Set<T> handlers) {
    this.handlers = handlers;
  }

  public void addHandler(T handler) {
    handlers.add(handler);
  }

  public void removeHandler(T handler) {
    handlers.remove(handler);
  }

  public Set<T> getHandlers() {
    return handlers;
  }
}
