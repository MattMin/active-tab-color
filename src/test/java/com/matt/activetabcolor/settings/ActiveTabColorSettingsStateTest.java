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
    assertEquals(100, state.tabCatScalePercent);
    assertEquals(100, state.tabCatSpeedPercent);
  }

  @Test
  void copyPreservesTabCatSettings() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();
    state.showTabCat = true;
    state.tabCat = "siri";
    state.tabCatScalePercent = 125;
    state.tabCatSpeedPercent = 175;

    ActiveTabColorSettingsState.PluginState copy = state.copy();

    assertTrue(copy.showTabCat);
    assertEquals("siri", copy.tabCat);
    assertEquals(125, copy.tabCatScalePercent);
    assertEquals(175, copy.tabCatSpeedPercent);
    assertEquals(state, copy);
  }

  @Test
  void equalityIncludesTabCatVisibility() {
    ActiveTabColorSettingsState.PluginState hidden = new ActiveTabColorSettingsState.PluginState();
    ActiveTabColorSettingsState.PluginState visible = new ActiveTabColorSettingsState.PluginState();
    visible.showTabCat = true;

    assertNotEquals(hidden, visible);
  }

  @Test
  void equalityIncludesTabCatScale() {
    ActiveTabColorSettingsState.PluginState medium = new ActiveTabColorSettingsState.PluginState();
    ActiveTabColorSettingsState.PluginState large = new ActiveTabColorSettingsState.PluginState();
    large.tabCatScalePercent = 125;

    assertNotEquals(medium, large);
  }

  @Test
  void equalityIncludesTabCatSpeed() {
    ActiveTabColorSettingsState.PluginState normal = new ActiveTabColorSettingsState.PluginState();
    ActiveTabColorSettingsState.PluginState fast = new ActiveTabColorSettingsState.PluginState();
    fast.tabCatSpeedPercent = 150;

    assertNotEquals(normal, fast);
  }

  @Test
  void invalidTabCatFallsBackToDefault() {
    assertEquals("siri", ActiveTabColorSettingsState.normalizeTabCat("siri"));
    assertEquals("luna", ActiveTabColorSettingsState.normalizeTabCat("luna"));
    assertEquals("timi", ActiveTabColorSettingsState.normalizeTabCat(null));
    assertEquals("timi", ActiveTabColorSettingsState.normalizeTabCat("unknown"));
  }

  @Test
  void invalidTabCatScaleIsClamped() {
    assertEquals(60, ActiveTabColorSettingsState.normalizeTabCatScalePercent(10));
    assertEquals(100, ActiveTabColorSettingsState.normalizeTabCatScalePercent(100));
    assertEquals(150, ActiveTabColorSettingsState.normalizeTabCatScalePercent(200));

    ActiveTabColorSettingsState.PluginState tooSmall = new ActiveTabColorSettingsState.PluginState();
    tooSmall.tabCatScalePercent = 10;
    assertEquals(60, tooSmall.copy().tabCatScalePercent);
  }

  @Test
  void invalidTabCatSpeedIsClamped() {
    assertEquals(50, ActiveTabColorSettingsState.normalizeTabCatSpeedPercent(10));
    assertEquals(100, ActiveTabColorSettingsState.normalizeTabCatSpeedPercent(100));
    assertEquals(200, ActiveTabColorSettingsState.normalizeTabCatSpeedPercent(300));

    ActiveTabColorSettingsState.PluginState tooFast = new ActiveTabColorSettingsState.PluginState();
    tooFast.tabCatSpeedPercent = 300;
    assertEquals(200, tooFast.copy().tabCatSpeedPercent);
  }
}
