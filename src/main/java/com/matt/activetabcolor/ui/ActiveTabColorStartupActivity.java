package com.matt.activetabcolor.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public final class ActiveTabColorStartupActivity implements StartupActivity.DumbAware {
  @Override
  public void runActivity(@NotNull Project project) {
    ActiveTabColorRefresh.refreshProject(project);
  }
}
