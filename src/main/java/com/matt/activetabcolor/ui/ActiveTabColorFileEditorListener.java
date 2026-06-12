package com.matt.activetabcolor.ui;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class ActiveTabColorFileEditorListener implements FileEditorManagerListener {
  @Override
  public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    ActiveTabColorRefresh.refreshFile(source.getProject(), file);
  }

  @Override
  public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    ActiveTabColorRefresh.refreshProject(source.getProject());
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    Project project = event.getManager().getProject();
    ActiveTabColorRefresh.refreshSelection(project, event.getOldFile(), event.getNewFile());
  }
}
