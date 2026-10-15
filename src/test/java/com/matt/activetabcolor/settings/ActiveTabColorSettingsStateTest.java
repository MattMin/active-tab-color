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
  @Test
  void newSettingsKeepCompatibleDefaultsAndCopyAllFields() {
    var state = new ActiveTabColorSettingsState.PluginState();
    assertTrue(state.activeOverridesRules);
    assertFalse(state.showActiveTabDot);
    assertEquals(1, state.active.outlineBorderWidth);
    var rule = new ActiveTabColorSettingsState.TabColorRule();
    assertEquals(1, rule.outlineBorderWidth);
    state.rules.add(rule);
    state.activeOverridesRules = false;
    state.showActiveTabDot = true;
    state.active.outlineBorderWidth = 4;
    rule.outlineBorderWidth = 6;
    var copy = state.copy();
    assertEquals(state, copy);
    assertEquals(state.hashCode(), copy.hashCode());
    assertEquals(4, copy.active.outlineBorderWidth);
    assertEquals(6, copy.rules.get(0).outlineBorderWidth);
    copy.activeOverridesRules = true;
    assertNotEquals(state, copy);
    copy = state.copy();
    copy.showActiveTabDot = false;
    assertNotEquals(state, copy);
    copy = state.copy();
    copy.active.outlineBorderWidth = 2;
    assertNotEquals(state, copy);
    copy = state.copy();
    copy.rules.get(0).outlineBorderWidth = 2;
    assertNotEquals(state, copy);
  }

  @Test
  void outlineWidthsAreClampedWhenLoadingState() {
    var state = new ActiveTabColorSettingsState.PluginState();
    state.active.outlineBorderWidth = 0;
    var rule = new ActiveTabColorSettingsState.TabColorRule();
    rule.outlineBorderWidth = 99;
    state.rules.add(rule);
    var service = new ActiveTabColorSettingsState();
    service.loadState(state);
    assertEquals(1, service.getState().active.outlineBorderWidth);
    assertEquals(6, service.getState().rules.get(0).outlineBorderWidth);
    assertEquals(3, ActiveTabColorSettingsState.normalizeOutlineBorderWidth(3));
  }
}
