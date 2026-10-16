/* (C) 2024 Igibaev */
package kz.aday.bot.configuration;

import java.util.EnumSet;
import java.util.Set;
import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;

public final class FeatureToggles {
  private static final Set<State> DEFAULT_DISABLED_STATES =
      EnumSet.of(
          State.SET_OFFICE_ATTENDANCE,
          State.STATISTICS_MENU,
          State.GET_ATTENDANCE_STATS,
          State.GET_ATTENDANCE_STATS_MONTH,
          State.GET_MY_ATTENDANCE_STATS,
          State.GET_MY_ATTENDANCE_STATS_MONTH,
          State.VIEW_POOL,
          State.SHARE_LUNCH);
  private static final Set<CallbackState> DEFAULT_DISABLED_CALLBACKS =
      EnumSet.of(
          CallbackState.ATTENDANCE_DAY_TODAY,
          CallbackState.ATTENDANCE_DAY_TOMORROW,
          CallbackState.ATTENDANCE_YES,
          CallbackState.ATTENDANCE_NO,
          CallbackState.POOL_CLAIM);

  private final Set<State> disabledStates;
  private final Set<CallbackState> disabledCallbacks;

  public FeatureToggles(Set<State> disabledStates, Set<CallbackState> disabledCallbacks) {
    this.disabledStates = Set.copyOf(disabledStates);
    this.disabledCallbacks = Set.copyOf(disabledCallbacks);
  }

  public static FeatureToggles defaults() {
    return new FeatureToggles(DEFAULT_DISABLED_STATES, DEFAULT_DISABLED_CALLBACKS);
  }

  public static FeatureToggles allEnabled() {
    return new FeatureToggles(Set.of(), Set.of());
  }

  public boolean isEnabled(State state) {
    return !disabledStates.contains(state);
  }

  public boolean isStateTextDisabled(String text) {
    return disabledStates.stream().anyMatch(state -> state.getDisplayName().equals(text));
  }

  public boolean isCallbackDisabled(String data) {
    String callbackName = CallbackState.extractName(data);
    return disabledCallbacks.stream().anyMatch(callback -> callback.name().equals(callbackName));
  }
}
