package com.matt.activetabcolor.model;

import com.matt.activetabcolor.settings.ActiveTabColorSettingsState;
import org.junit.jupiter.api.Test;

import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TabStyleResolverTest {
  @Test
  void defaultStateDoesNotApplyAnyColor() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();

    TabStyle style = TabStyleResolver.resolve(state, "UserService.java", false);

    assertNull(style.getBackgroundRgb());
    assertNull(style.getUnderlineBorderRgb());
    assertNull(style.getOutlineBorderRgb());
  }

  @Test
  void regexMatchesDisplayedTabName() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();
    ActiveTabColorSettingsState.TabColorRule rule = rule("services", ".*Service\\.java");
    rule.backgroundRgb = 0x112233;
    state.rules.add(rule);

    TabStyle style = TabStyleResolver.resolve(state, "UserService.java", false);

    assertEquals(0x112233, style.getBackgroundRgb());
  }

  @Test
  void firstMatchingRuleWins() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();
    ActiveTabColorSettingsState.TabColorRule first = rule("first", ".*Service.*");
    first.backgroundRgb = 0x111111;
    ActiveTabColorSettingsState.TabColorRule second = rule("second", "User.*");
    second.backgroundRgb = 0x222222;
    state.rules.add(first);
    state.rules.add(second);

    TabStyle style = TabStyleResolver.resolve(state, "UserService.java", false);

    assertEquals(0x111111, style.getBackgroundRgb());
  }

  @Test
  void activeColorsOverrideConfiguredItemsOnly() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();
    ActiveTabColorSettingsState.TabColorRule rule = rule("rule", ".*Service.*");
    rule.backgroundRgb = 0x111111;
    rule.underlineBorderRgb = 0x222222;
    state.rules.add(rule);
    state.active.outlineBorderRgb = 0x333333;

    TabStyle style = TabStyleResolver.resolve(state, "UserService.java", true);

    assertEquals(0x111111, style.getBackgroundRgb());
    assertEquals(0x222222, style.getUnderlineBorderRgb());
    assertEquals(0x333333, style.getOutlineBorderRgb());
  }

  @Test
  void disabledPluginReturnsEmptyStyle() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();
    state.enabled = false;
    state.active.backgroundRgb = 0x111111;

    TabStyle style = TabStyleResolver.resolve(state, "UserService.java", true);

    assertNull(style.getBackgroundRgb());
  }

  @Test
  void invalidEnabledRegexFailsValidation() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();
    state.rules.add(rule("bad", "["));

    assertThrows(PatternSyntaxException.class, () -> TabStyleResolver.validateRules(state));
  }

  private static ActiveTabColorSettingsState.TabColorRule rule(String name, String pattern) {
    ActiveTabColorSettingsState.TabColorRule rule = new ActiveTabColorSettingsState.TabColorRule();
    rule.name = name;
    rule.pattern = pattern;
    rule.enabled = true;
    return rule;
  }
}
