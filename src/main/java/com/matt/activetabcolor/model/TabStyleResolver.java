package com.matt.activetabcolor.model;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.matt.activetabcolor.settings.ActiveTabColorSettingsState;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class TabStyleResolver {
  private TabStyleResolver() {
  }

  public static @NotNull TabStyle resolve(@NotNull Project project, @NotNull VirtualFile file) {
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    if (!state.enabled) {
      return TabStyle.EMPTY;
    }
    String tabName = EditorTabPresentationUtil.getEditorTabTitle(project, file);
    boolean active = isSelectedFile(project, file);
    return resolve(state, tabName, active);
  }

  public static @NotNull TabStyle resolve(@NotNull ActiveTabColorSettingsState.PluginState state,
                                          @NotNull String tabName,
                                          boolean active) {
    if (!state.enabled) {
      return TabStyle.EMPTY;
    }

    TabStyle ruleStyle = firstMatchingRuleStyle(state, tabName);
    if (!active) {
      return ruleStyle;
    }

    TabStyle activeStyle = toStyle(state.active);
    return ruleStyle.overlay(activeStyle);
  }

  public static void validateRules(@NotNull ActiveTabColorSettingsState.PluginState state) throws PatternSyntaxException {
    if (state.rules == null) {
      return;
    }
    for (ActiveTabColorSettingsState.TabColorRule rule : state.rules) {
      if (rule == null || !rule.enabled || rule.pattern == null || rule.pattern.isBlank()) {
        continue;
      }
      Pattern.compile(rule.pattern);
    }
  }

  private static @NotNull TabStyle firstMatchingRuleStyle(@NotNull ActiveTabColorSettingsState.PluginState state,
                                                         @NotNull String tabName) {
    if (state.rules == null) {
      return TabStyle.EMPTY;
    }
    for (ActiveTabColorSettingsState.TabColorRule rule : state.rules) {
      if (matches(rule, tabName)) {
        return toStyle(rule);
      }
    }
    return TabStyle.EMPTY;
  }

  private static boolean matches(ActiveTabColorSettingsState.TabColorRule rule, String tabName) {
    if (rule == null || !rule.enabled || rule.pattern == null || rule.pattern.isBlank()) {
      return false;
    }
    try {
      return Pattern.compile(rule.pattern).matcher(tabName).find();
    }
    catch (PatternSyntaxException ignored) {
      return false;
    }
  }

  private static @NotNull TabStyle toStyle(ActiveTabColorSettingsState.ColorSettings settings) {
    if (settings == null || settings.isEmpty()) {
      return TabStyle.EMPTY;
    }
    return new TabStyle(
      settings.backgroundRgb,
      settings.underlineBorderRgb,
      settings.outlineBorderRgb
    );
  }

  private static boolean isSelectedFile(Project project, VirtualFile file) {
    for (VirtualFile selectedFile : FileEditorManager.getInstance(project).getSelectedFiles()) {
      if (file.equals(selectedFile)) {
        return true;
      }
    }
    return false;
  }
}
