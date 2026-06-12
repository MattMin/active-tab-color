package com.matt.activetabcolor.ui;

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.matt.activetabcolor.model.TabStyle;
import com.matt.activetabcolor.model.TabStyleResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public final class ActiveTabColorProvider implements EditorTabColorProvider, DumbAware {
  @Override
  public @Nullable Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file) {
    TabStyle style = TabStyleResolver.resolve(project, file);
    return ColorUtil.fromRgb(style.getBackgroundRgb());
  }
}
