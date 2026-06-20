package com.matt.activetabcolor.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.matt.activetabcolor.settings.ActiveTabColorSettingsState;
import org.jetbrains.annotations.NotNull;

public final class ToggleTabCatAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    ActiveTabColorSettingsState settings = ActiveTabColorSettingsState.getInstance();
    ActiveTabColorSettingsState.PluginState state = settings.getState().copy();
    state.showTabCat = !state.showTabCat;
    settings.setState(state);
    ActiveTabColorRefresh.refreshAllOpenProjects();
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    event.getPresentation().setText(state != null && state.showTabCat ? "Hide Tab Cat" : "Show Tab Cat");
  }
}
