package com.matt.activetabcolor.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;

public final class ActiveTabColorRefresh {
  private ActiveTabColorRefresh() {
  }

  public static void refreshAllOpenProjects() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      refreshProject(project);
    }
  }

  public static void refreshProject(Project project) {
    if (project == null || project.isDisposed()) {
      return;
    }
    Runnable refresh = () -> {
      if (project.isDisposed()) {
        return;
      }
      FileEditorManager manager = FileEditorManager.getInstance(project);
      for (VirtualFile file : manager.getOpenFiles()) {
        manager.updateFilePresentation(file);
        manager.updateFileColor(file);
      }
      ActiveTabColorDecorator.refreshProject(project);
      ActiveTabColorDecorator.refreshProjectLater(project, 100);
      ActiveTabColorDecorator.refreshProjectLater(project, 300);
    };

    if (ApplicationManager.getApplication().isDispatchThread()) {
      refresh.run();
    }
    else {
      ApplicationManager.getApplication().invokeLater(refresh);
    }
  }

  public static void refreshFile(Project project, VirtualFile file) {
    if (project == null || project.isDisposed() || file == null) {
      return;
    }
    Runnable refresh = () -> {
      if (project.isDisposed()) {
        return;
      }
      FileEditorManager manager = FileEditorManager.getInstance(project);
      manager.updateFilePresentation(file);
      manager.updateFileColor(file);
      ActiveTabColorDecorator.refreshProject(project);
      ActiveTabColorDecorator.refreshProjectLater(project, 100);
      ActiveTabColorDecorator.refreshProjectLater(project, 300);
    };
    if (ApplicationManager.getApplication().isDispatchThread()) {
      refresh.run();
    }
    else {
      ApplicationManager.getApplication().invokeLater(refresh);
    }
  }
}
