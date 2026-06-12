package com.matt.activetabcolor.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.ui.JBUI;
import com.matt.activetabcolor.model.TabStyle;
import com.matt.activetabcolor.model.TabStyleResolver;
import com.matt.activetabcolor.settings.ActiveTabColorSettingsState;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ActiveTabColorDecorator {
  private static final String ORIGINAL_BORDER_KEY = "activeTabColor.originalBorder";

  private ActiveTabColorDecorator() {
  }

  public static void refreshProject(Project project) {
    if (project == null || project.isDisposed()) {
      return;
    }
    Runnable refresh = () -> {
      if (project.isDisposed()) {
        return;
      }
      FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
      JComponent component = manager == null ? null : manager.getComponent();
      if (component != null) {
        refreshComponent(component);
      }
    };

    if (ApplicationManager.getApplication().isDispatchThread()) {
      refresh.run();
    }
    else {
      ApplicationManager.getApplication().invokeLater(refresh);
    }
  }

  public static void refreshProjectLater(Project project, long delayMillis) {
    if (project == null || project.isDisposed()) {
      return;
    }
    EdtExecutorService.getScheduledExecutorInstance().schedule(
      () -> refreshProject(project),
      delayMillis,
      TimeUnit.MILLISECONDS
    );
  }

  private static void refreshComponent(Component component) {
    if (component instanceof TabLabel) {
      refreshTabLabel((TabLabel)component);
    }
    if (component instanceof java.awt.Container) {
      Component[] children = ((java.awt.Container)component).getComponents();
      for (Component child : children) {
        refreshComponent(child);
      }
    }
  }

  private static void refreshTabLabel(TabLabel label) {
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    String tabName = label.getInfo().getText();
    boolean active = isSelected(label);
    TabStyle style = TabStyleResolver.resolve(state, tabName == null ? "" : tabName, active);

    Integer backgroundRgb = style.getBackgroundRgb();
    Integer underlineRgb = style.getUnderlineBorderRgb();
    Integer outlineRgb = style.getOutlineBorderRgb();
    if (backgroundRgb == null && underlineRgb == null && outlineRgb == null) {
      restoreOriginalBorder(label);
      return;
    }

    Border current = label.getBorder();
    Border original = getOriginalBorder(label, current);
    ActiveTabOverlayBorder overlay = new ActiveTabOverlayBorder(original, backgroundRgb, underlineRgb, outlineRgb);
    if (!overlay.equals(current)) {
      label.setBorder(overlay);
    }
    label.repaint();
  }

  private static boolean isSelected(TabLabel label) {
    JBTabsImpl tabs = findTabs(label);
    return tabs != null && label.getInfo() == tabs.getSelectedInfo();
  }

  private static JBTabsImpl findTabs(Component component) {
    Component current = component.getParent();
    while (current != null) {
      if (current instanceof JBTabsImpl) {
        return (JBTabsImpl)current;
      }
      current = current.getParent();
    }
    return null;
  }

  private static Border getOriginalBorder(TabLabel label, Border current) {
    Object saved = label.getClientProperty(ORIGINAL_BORDER_KEY);
    if (saved instanceof Border || saved == null) {
      if (current instanceof ActiveTabOverlayBorder) {
        return (Border)saved;
      }
      label.putClientProperty(ORIGINAL_BORDER_KEY, current);
      return current;
    }
    return current;
  }

  private static void restoreOriginalBorder(TabLabel label) {
    Object saved = label.getClientProperty(ORIGINAL_BORDER_KEY);
    if (saved instanceof Border || saved == null) {
      Border current = label.getBorder();
      if (current instanceof ActiveTabOverlayBorder) {
        label.setBorder((Border)saved);
        label.putClientProperty(ORIGINAL_BORDER_KEY, null);
        label.repaint();
      }
    }
  }

  private static final class ActiveTabOverlayBorder implements Border {
    private final Border delegate;
    private final Integer backgroundRgb;
    private final Integer underlineRgb;
    private final Integer outlineRgb;

    private ActiveTabOverlayBorder(Border delegate, Integer backgroundRgb, Integer underlineRgb, Integer outlineRgb) {
      this.delegate = delegate;
      this.backgroundRgb = ActiveTabColorSettingsState.normalizeRgb(backgroundRgb);
      this.underlineRgb = ActiveTabColorSettingsState.normalizeRgb(underlineRgb);
      this.outlineRgb = ActiveTabColorSettingsState.normalizeRgb(outlineRgb);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      if (delegate != null) {
        delegate.paintBorder(c, g, x, y, width, height);
      }
      if (backgroundRgb != null) {
        paintRoundedBackground(g, x, y, width, height);
      }
      if (outlineRgb != null) {
        g.setColor(ColorUtil.fromRgb(outlineRgb));
        int inset = JBUI.scale(2);
        int arc = JBUI.scale(8);
        g.drawRoundRect(x + inset, y + inset, width - 1 - inset * 2, height - 1 - inset * 2, arc, arc);
      }
      if (underlineRgb != null) {
        int lineHeight = Math.max(1, JBUI.scale(2));
        g.setColor(ColorUtil.fromRgb(underlineRgb));
        int inset = JBUI.scale(6);
        g.fillRoundRect(x + inset, y + height - lineHeight - JBUI.scale(2), width - inset * 2, lineHeight, lineHeight, lineHeight);
      }
    }

    private void paintRoundedBackground(Graphics g, int x, int y, int width, int height) {
      Graphics2D g2 = (Graphics2D)g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ColorUtil.fromRgb(backgroundRgb));
        int insetX = JBUI.scale(2);
        int insetY = JBUI.scale(2);
        int arc = JBUI.scale(8);
        g2.fillRoundRect(x + insetX, y + insetY, width - insetX * 2, height - insetY * 2, arc, arc);
      }
      finally {
        g2.dispose();
      }
    }

    @Override
    public Insets getBorderInsets(Component c) {
      return delegate == null ? JBUI.emptyInsets() : delegate.getBorderInsets(c);
    }

    @Override
    public boolean isBorderOpaque() {
      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ActiveTabOverlayBorder)) return false;
      ActiveTabOverlayBorder that = (ActiveTabOverlayBorder)o;
      return Objects.equals(delegate, that.delegate) &&
             Objects.equals(backgroundRgb, that.backgroundRgb) &&
             Objects.equals(underlineRgb, that.underlineRgb) &&
             Objects.equals(outlineRgb, that.outlineRgb);
    }

    @Override
    public int hashCode() {
      return Objects.hash(delegate, backgroundRgb, underlineRgb, outlineRgb);
    }
  }
}
