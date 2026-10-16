/* (C) 2024 Igibaev */
package kz.aday.bot.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kz.aday.bot.bot.handler.callbackHandlers.CallbackState;
import kz.aday.bot.bot.handler.stateHandlers.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FeatureTogglesTest {

  private static final String TODAY_SUFFIX = ":TODAY";
  private static final String UNKNOWN_TEXT = "some raw text";

  private final FeatureToggles defaults = FeatureToggles.defaults();
  private final FeatureToggles allEnabled = FeatureToggles.allEnabled();

  @ParameterizedTest
  @EnumSource(
      value = State.class,
      names = {
        "SET_OFFICE_ATTENDANCE",
        "STATISTICS_MENU",
        "GET_ATTENDANCE_STATS",
        "GET_ATTENDANCE_STATS_MONTH",
        "GET_MY_ATTENDANCE_STATS",
        "GET_MY_ATTENDANCE_STATS_MONTH",
        "VIEW_POOL",
        "SHARE_LUNCH"
      })
  void defaults_givenHiddenState_whenChecked_thenStateIsDisabled(State state) {
    assertFalse(defaults.isEnabled(state));
    assertTrue(defaults.isStateTextDisabled(state.getDisplayName()));
  }

  @ParameterizedTest
  @EnumSource(
      value = State.class,
      names = {
        "PROFILE_MENU",
        "MENU_CATEGORY_ORDER",
        "WHO_WILL_COME_TO_OFFICE",
        "CREATE_ORDER",
        "GET_ORDER"
      })
  void defaults_givenVisibleState_whenChecked_thenStateIsEnabled(State state) {
    assertTrue(defaults.isEnabled(state));
    assertFalse(defaults.isStateTextDisabled(state.getDisplayName()));
  }

  @Test
  void defaults_givenTextNotMatchingAnyState_whenChecked_thenTextIsNotDisabled() {
    assertFalse(defaults.isStateTextDisabled(UNKNOWN_TEXT));
  }

  @ParameterizedTest
  @EnumSource(
      value = CallbackState.class,
      names = {
        "ATTENDANCE_DAY_TODAY",
        "ATTENDANCE_DAY_TOMORROW",
        "ATTENDANCE_YES",
        "ATTENDANCE_NO",
        "POOL_CLAIM"
      })
  void defaults_givenHiddenCallback_whenChecked_thenCallbackIsDisabledWithAndWithoutSuffix(
      CallbackState callback) {
    assertTrue(defaults.isCallbackDisabled(callback.name()));
    assertTrue(defaults.isCallbackDisabled(callback.name() + TODAY_SUFFIX));
  }

  @ParameterizedTest
  @EnumSource(
      value = CallbackState.class,
      names = {"ADD_ITEM_TO_ORDER", "CANCEL", "SUBMIT_ORDER", "WHO_COMES_TODAY"})
  void defaults_givenVisibleCallback_whenChecked_thenCallbackIsNotDisabled(CallbackState callback) {
    assertFalse(defaults.isCallbackDisabled(callback.name() + TODAY_SUFFIX));
  }

  @ParameterizedTest
  @EnumSource(State.class)
  void allEnabled_givenAnyState_whenChecked_thenStateIsEnabled(State state) {
    assertTrue(allEnabled.isEnabled(state));
    assertFalse(allEnabled.isStateTextDisabled(state.getDisplayName()));
  }

  @ParameterizedTest
  @EnumSource(CallbackState.class)
  void allEnabled_givenAnyCallback_whenChecked_thenCallbackIsNotDisabled(CallbackState callback) {
    assertFalse(allEnabled.isCallbackDisabled(callback.name() + TODAY_SUFFIX));
  }
}
