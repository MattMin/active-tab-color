package com.matt.activetabcolor.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

public final class ActiveTabColorStartupActivity implements ProjectActivity, DumbAware {
  @Override
  public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    ActiveTabColorRefresh.refreshProject(project);
    return Unit.INSTANCE;
  }
}
