package com.matt.activetabcolor.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class ActiveTabColorRefresh {
  private static final Key<PendingRefresh> PENDING_REFRESH_KEY = Key.create("activeTabColor.pendingRefresh");

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
    requestRefresh(project, pending -> {
      pending.allFiles = true;
      pending.delayedDecorations = true;
    });
  }

  public static void refreshFile(Project project, VirtualFile file) {
    if (project == null || project.isDisposed() || file == null) {
      return;
    }
    requestRefresh(project, pending -> {
      pending.files.add(file);
      pending.delayedDecorations = true;
    });
  }

  public static void refreshSelection(Project project, VirtualFile oldFile, VirtualFile newFile) {
    if (project == null || project.isDisposed()) {
      return;
    }
    requestRefresh(project, pending -> {
      if (oldFile != null) {
        pending.files.add(oldFile);
      }
      if (newFile != null) {
        pending.files.add(newFile);
      }
    });
  }

  private static void requestRefresh(Project project, Consumer<PendingRefresh> update) {
    PendingRefresh pending = getPendingRefresh(project);
    boolean shouldSchedule;
    synchronized (pending) {
      update.accept(pending);
      shouldSchedule = !pending.scheduled;
      pending.scheduled = true;
    }
    if (shouldSchedule) {
      ApplicationManager.getApplication().invokeLater(() -> runPendingRefresh(project, pending));
    }
  }

  private static PendingRefresh getPendingRefresh(Project project) {
    PendingRefresh pending = project.getUserData(PENDING_REFRESH_KEY);
    if (pending != null) {
      return pending;
    }
    PendingRefresh created = new PendingRefresh();
    project.putUserData(PENDING_REFRESH_KEY, created);
    return created;
  }

  private static void runPendingRefresh(Project project, PendingRefresh pending) {
    PendingRefreshSnapshot snapshot;
    synchronized (pending) {
      snapshot = new PendingRefreshSnapshot(pending.allFiles, pending.delayedDecorations, new LinkedHashSet<>(pending.files));
      pending.allFiles = false;
      pending.delayedDecorations = false;
      pending.files.clear();
      pending.scheduled = false;
    }

    if (project.isDisposed()) {
      return;
    }

    FileEditorManager manager = FileEditorManager.getInstance(project);
    if (snapshot.allFiles()) {
      for (VirtualFile file : manager.getOpenFiles()) {
        updateFile(manager, file);
      }
    }
    else {
      for (VirtualFile file : snapshot.files()) {
        updateFile(manager, file);
      }
    }
    ActiveTabColorDecorator.refreshProject(project);
    if (snapshot.delayedDecorations()) {
      ActiveTabColorDecorator.refreshProjectLater(project, 100);
    }
  }

  private static void updateFile(FileEditorManager manager, VirtualFile file) {
    if (file == null || !file.isValid()) {
      return;
    }
    manager.updateFilePresentation(file);
    manager.updateFileColor(file);
  }

  private static final class PendingRefresh {
    private boolean scheduled;
    private boolean allFiles;
    private boolean delayedDecorations;
    private final Set<VirtualFile> files = new LinkedHashSet<>();
  }

  private record PendingRefreshSnapshot(boolean allFiles,
                                        boolean delayedDecorations,
                                        Set<VirtualFile> files) {
  }
}
