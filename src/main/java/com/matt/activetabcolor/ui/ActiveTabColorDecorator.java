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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
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
      JComponent component = FileEditorManagerEx.getInstanceEx(project).getComponent();
      refreshComponent(component);
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
    if (component instanceof TabLabel label) {
      refreshTabLabel(label);
    }
    if (component instanceof java.awt.Container container) {
      Component[] children = container.getComponents();
      for (Component child : children) {
        refreshComponent(child);
      }
    }
  }

  private static void refreshTabLabel(TabLabel label) {
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    String tabName = label.getInfo().getText();
    boolean active = isSelected(label);
    TabStyle style = TabStyleResolver.resolve(state, tabName, active);

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
      if (current instanceof JBTabsImpl tabs) {
        return tabs;
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

  private record ActiveTabOverlayBorder(Border delegate,
                                        Integer backgroundRgb,
                                        Integer underlineRgb,
                                        Integer outlineRgb) implements Border {
    private ActiveTabOverlayBorder {
      backgroundRgb = ActiveTabColorSettingsState.normalizeRgb(backgroundRgb);
      underlineRgb = ActiveTabColorSettingsState.normalizeRgb(underlineRgb);
      outlineRgb = ActiveTabColorSettingsState.normalizeRgb(outlineRgb);
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
        int arc = JBUI.scale(4);
        g.fillRoundRect(x + inset, y + height - lineHeight - JBUI.scale(2), width - inset * 2, lineHeight, arc, arc);
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
  }
}
