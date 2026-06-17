package com.matt.activetabcolor.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveTabColorSettingsStateTest {
  @Test
  void defaultStateKeepsTabCatDisabled() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();

    assertFalse(state.showTabCat);
    assertEquals("timi", state.tabCat);
  }

  @Test
  void copyPreservesTabCatSetting() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();
    state.showTabCat = true;
    state.tabCat = "siri";

    ActiveTabColorSettingsState.PluginState copy = state.copy();

    assertTrue(copy.showTabCat);
    assertEquals("siri", copy.tabCat);
    assertEquals(state, copy);
  }

  @Test
  void equalityIncludesTabCatSetting() {
    ActiveTabColorSettingsState.PluginState hidden = new ActiveTabColorSettingsState.PluginState();
    ActiveTabColorSettingsState.PluginState visible = new ActiveTabColorSettingsState.PluginState();
    visible.showTabCat = true;

    assertNotEquals(hidden, visible);
  }

  @Test
  void invalidTabCatFallsBackToDefault() {
    assertEquals("siri", ActiveTabColorSettingsState.normalizeTabCat("siri"));
    assertEquals("luna", ActiveTabColorSettingsState.normalizeTabCat("luna"));
    assertEquals("timi", ActiveTabColorSettingsState.normalizeTabCat(null));
    assertEquals("timi", ActiveTabColorSettingsState.normalizeTabCat("unknown"));
  }
}
