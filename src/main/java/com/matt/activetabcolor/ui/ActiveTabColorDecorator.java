package com.matt.activetabcolor.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.ui.JBUI;
import com.matt.activetabcolor.model.TabStyle;
import com.matt.activetabcolor.model.TabStyleResolver;
import com.matt.activetabcolor.settings.ActiveTabColorSettingsState;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.geom.RoundRectangle2D;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.concurrent.TimeUnit;

public final class ActiveTabColorDecorator {
  private static final String ORIGINAL_BORDER_KEY = "activeTabColor.originalBorder";
  private static final String LAST_SELECTED_INFO_KEY = "activeTabColor.lastSelectedInfo";

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

  public static void refreshSelection(Project project) {
    if (project == null || project.isDisposed()) {
      return;
    }
    Runnable refresh = () -> {
      if (project.isDisposed()) {
        return;
      }
      JComponent component = FileEditorManagerEx.getInstanceEx(project).getComponent();
      refreshSelectionComponent(component);
    };

    if (ApplicationManager.getApplication().isDispatchThread()) {
      refresh.run();
    }
    else {
      ApplicationManager.getApplication().invokeLater(refresh);
    }
  }

  private static void refreshComponent(Component component) {
    if (component instanceof JBTabs tabs) {
      refreshTabs(tabs);
      return;
    }
    if (component instanceof java.awt.Container container) {
      Component[] children = container.getComponents();
      for (Component child : children) {
        refreshComponent(child);
      }
    }
  }

  private static void refreshTabs(JBTabs tabs) {
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    TabInfo selectedInfo = tabs.getSelectedInfo();
    for (TabInfo tabInfo : tabs.getTabs()) {
      Component label = tabs.getTabLabel(tabInfo);
      if (label instanceof JComponent labelComponent) {
        refreshTabLabel(labelComponent, tabInfo, tabInfo == selectedInfo, state);
      }
    }
    rememberSelectedInfo(tabs, selectedInfo);
  }

  private static void refreshSelectionComponent(Component component) {
    if (component instanceof JBTabs tabs) {
      refreshSelectedTabs(tabs);
      return;
    }
    if (component instanceof java.awt.Container container) {
      Component[] children = container.getComponents();
      for (Component child : children) {
        refreshSelectionComponent(child);
      }
    }
  }

  private static void refreshSelectedTabs(JBTabs tabs) {
    TabInfo selectedInfo = tabs.getSelectedInfo();
    TabInfo previousInfo = getLastSelectedInfo(tabs);
    if (previousInfo == null) {
      refreshTabs(tabs);
      return;
    }

    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    refreshTabInfo(tabs, previousInfo, previousInfo == selectedInfo, state);
    if (selectedInfo != previousInfo) {
      refreshTabInfo(tabs, selectedInfo, true, state);
    }
    rememberSelectedInfo(tabs, selectedInfo);
  }

  private static void refreshTabInfo(JBTabs tabs,
                                     TabInfo tabInfo,
                                     boolean active,
                                     ActiveTabColorSettingsState.PluginState state) {
    if (tabInfo == null || !tabs.getTabs().contains(tabInfo)) {
      return;
    }
    Component label = tabs.getTabLabel(tabInfo);
    if (label instanceof JComponent labelComponent) {
      refreshTabLabel(labelComponent, tabInfo, active, state);
    }
  }

  private static TabInfo getLastSelectedInfo(JBTabs tabs) {
    Object saved = tabs.getComponent().getClientProperty(LAST_SELECTED_INFO_KEY);
    return saved instanceof TabInfo ? (TabInfo)saved : null;
  }

  private static void rememberSelectedInfo(JBTabs tabs, TabInfo selectedInfo) {
    tabs.getComponent().putClientProperty(LAST_SELECTED_INFO_KEY, selectedInfo);
  }

  private static void refreshTabLabel(JComponent label,
                                      TabInfo tabInfo,
                                      boolean active,
                                      ActiveTabColorSettingsState.PluginState state) {
    String tabName = tabInfo.getText();
    TabStyle style = TabStyleResolver.resolve(state, tabName, active);

    Integer backgroundRgb = style.getBackgroundRgb();
    Integer underlineRgb = style.getUnderlineBorderRgb();
    Integer outlineRgb = style.getOutlineBorderRgb();
    boolean showDot = state.enabled && state.showActiveTabDot && active;
    if (style.isEmpty() && !showDot) {
      restoreOriginalBorder(label);
      return;
    }

    Border current = label.getBorder();
    Border original = getOriginalBorder(label, current);
    ActiveTabOverlayBorder overlay = new ActiveTabOverlayBorder(original, backgroundRgb, underlineRgb, outlineRgb, style.outlineBorderWidth(), showDot);
    if (!overlay.equals(current)) {
      label.setBorder(overlay);
    }
    label.repaint();
  }

  private static Border getOriginalBorder(JComponent label, Border current) {
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

  private static void restoreOriginalBorder(JComponent label) {
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

  record ActiveTabOverlayBorder(Border delegate,
                                        Integer backgroundRgb,
                                        Integer underlineRgb,
                                        Integer outlineRgb,
                                        int outlineWidth,
                                        boolean showDot) implements Border {
    ActiveTabOverlayBorder {
      backgroundRgb = ActiveTabColorSettingsState.normalizeRgb(backgroundRgb);
      underlineRgb = ActiveTabColorSettingsState.normalizeRgb(underlineRgb);
      outlineRgb = ActiveTabColorSettingsState.normalizeRgb(outlineRgb);
      outlineWidth = ActiveTabColorSettingsState.normalizeOutlineBorderWidth(outlineWidth);
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
        Graphics2D g2 = (Graphics2D)g.create();
        try {
          float strokeWidth = JBUI.scale((float)outlineWidth);
          float inset = JBUI.scale(2) + strokeWidth / 2;
          float rectWidth = width - inset * 2;
          float rectHeight = height - inset * 2;
          if (rectWidth > 0 && rectHeight > 0) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ColorUtil.fromRgb(outlineRgb));
            g2.setStroke(new BasicStroke(strokeWidth));
            g2.draw(new RoundRectangle2D.Float(x + inset, y + inset, rectWidth, rectHeight,
                                             JBUI.scale(8), JBUI.scale(8)));
          }
        }
        finally {
          g2.dispose();
        }
      }
      if (underlineRgb != null) {
        int lineHeight = Math.max(1, JBUI.scale(2));
        g.setColor(ColorUtil.fromRgb(underlineRgb));
        int inset = JBUI.scale(6);
        int arc = JBUI.scale(4);
        g.fillRoundRect(x + inset, y + height - lineHeight - JBUI.scale(2), width - inset * 2, lineHeight, arc, arc);
      }
      if (showDot) {
        paintDot(g, x, y);
      }
    }

    private void paintDot(Graphics g, int x, int y) {
      Graphics2D g2 = (Graphics2D)g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(JBColor.namedColor("Component.accentColor", JBColor.BLUE));
        g2.fillOval(x + JBUI.scale(4), y + JBUI.scale(4), JBUI.scale(6), JBUI.scale(6));
      }
      finally {
        g2.dispose();
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
