package com.matt.activetabcolor.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.ui.JBUI;
import com.matt.activetabcolor.settings.ActiveTabColorSettingsState;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ActiveTabCatDecorator {
  private static final Logger LOG = Logger.getInstance(ActiveTabCatDecorator.class);
  private static final String CAT_OVERLAY_KEY = "activeTabColor.catOverlay";
  private static final int TIMER_DELAY_MILLIS = 30;
  private static final int BASE_CAT_WIDTH = 44;
  private static final int BASE_CAT_HEIGHT = 31;
  private static final double WALK_SPEED = 2.0d;
  private static final double RUN_SPEED = 4.2d;
  private static final int RUN_DISTANCE = 120;
  private static final int WALK_FINISH_DISTANCE = 80;
  private static final int TURN_DURATION_FRAMES = 5;
  private static final int BLINK_AFTER_MILLIS = 5_000;
  private static final int BLINK_DURATION_MILLIS = 500;
  private static final Map<String, CatSprites> CAT_SPRITES = new ConcurrentHashMap<>();

  private ActiveTabCatDecorator() {
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
    if (component instanceof JBTabs tabs) {
      refreshTabs(tabs);
      return;
    }
    if (component instanceof Container container) {
      Component[] children = container.getComponents();
      for (Component child : children) {
        refreshComponent(child);
      }
    }
  }

  private static void refreshTabs(JBTabs tabs) {
    JComponent tabsComponent = tabs.getComponent();
    if (isTabCatEnabled()) {
      getOrCreateOverlay(tabsComponent).refresh(tabs);
    }
    else {
      removeOverlay(tabsComponent);
    }
  }

  private static boolean isTabCatEnabled() {
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    return state != null && state.showTabCat;
  }

  private static CatOverlay getOrCreateOverlay(JComponent tabsComponent) {
    Object saved = tabsComponent.getClientProperty(CAT_OVERLAY_KEY);
    if (saved instanceof CatOverlay overlay && !overlay.isDisposed()) {
      return overlay;
    }
    CatOverlay overlay = new CatOverlay(tabsComponent);
    tabsComponent.putClientProperty(CAT_OVERLAY_KEY, overlay);
    return overlay;
  }

  private static void removeOverlay(JComponent tabsComponent) {
    Object saved = tabsComponent.getClientProperty(CAT_OVERLAY_KEY);
    if (saved instanceof CatOverlay overlay) {
      overlay.dispose();
    }
  }

  private static final class CatOverlay extends JComponent {
    private final JComponent tabsComponent;
    private final Timer animationTimer;
    private final Timer blinkStartTimer;
    private final Timer blinkFinishTimer;
    private final ComponentAdapter componentListener;
    private final HierarchyBoundsAdapter hierarchyBoundsListener;
    private final HierarchyListener hierarchyListener;
    private JLayeredPane layeredPane;
    private JBTabs lastTabs;
    private double currentX;
    private double currentY;
    private int targetX;
    private int targetY;
    private int direction = 1;
    private int turnTargetDirection = 1;
    private int animationFrame;
    private boolean initialized;
    private boolean refreshScheduled;
    private boolean disposed;
    private CatPose pose = CatPose.SITTING;

    private CatOverlay(JComponent tabsComponent) {
      this.tabsComponent = tabsComponent;
      setOpaque(false);
      setFocusable(false);
      animationTimer = new Timer(TIMER_DELAY_MILLIS, e -> animate());
      blinkStartTimer = new Timer(BLINK_AFTER_MILLIS, e -> startBlink());
      blinkStartTimer.setRepeats(false);
      blinkFinishTimer = new Timer(BLINK_DURATION_MILLIS, e -> finishBlink());
      blinkFinishTimer.setRepeats(false);
      componentListener = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
          scheduleTargetRefresh();
        }

        @Override
        public void componentResized(ComponentEvent e) {
          scheduleTargetRefresh();
        }

        @Override
        public void componentShown(ComponentEvent e) {
          scheduleTargetRefresh();
        }

        @Override
        public void componentHidden(ComponentEvent e) {
          dispose();
        }
      };
      hierarchyBoundsListener = new HierarchyBoundsAdapter() {
        @Override
        public void ancestorMoved(HierarchyEvent e) {
          scheduleTargetRefresh();
        }

        @Override
        public void ancestorResized(HierarchyEvent e) {
          scheduleTargetRefresh();
        }
      };
      hierarchyListener = e -> {
        if ((e.getChangeFlags() & (HierarchyEvent.SHOWING_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) == 0) {
          return;
        }
        if (!tabsComponent.isShowing() || !tabsComponent.isDisplayable()) {
          dispose();
        }
      };
      tabsComponent.addComponentListener(componentListener);
      tabsComponent.addHierarchyBoundsListener(hierarchyBoundsListener);
      tabsComponent.addHierarchyListener(hierarchyListener);
    }

    private void refresh(JBTabs tabs) {
      if (disposed) {
        return;
      }
      lastTabs = tabs;
      if (!tabsComponent.isShowing()) {
        dispose();
        return;
      }
      JLayeredPane pane = findLayeredPane(tabsComponent);
      if (pane == null) {
        dispose();
        return;
      }
      installOn(pane);

      Point target = findCatTarget(tabs, pane);
      if (target == null) {
        setVisible(false);
        stopAllTimers();
        pose = CatPose.SITTING;
        animationFrame = 0;
        return;
      }
      setVisible(true);
      moveTo(target.x, target.y);
    }

    private void installOn(JLayeredPane pane) {
      if (layeredPane != pane) {
        if (layeredPane != null) {
          layeredPane.remove(this);
          layeredPane.repaint();
        }
        layeredPane = pane;
        layeredPane.add(this, JLayeredPane.POPUP_LAYER);
      }
      Rectangle bounds = layeredPane.getBounds();
      setBounds(0, 0, bounds.width, bounds.height);
    }

    private Point findCatTarget(JBTabs tabs, JLayeredPane pane) {
      TabInfo selectedInfo = tabs.getSelectedInfo();
      if (selectedInfo == null) {
        return null;
      }
      Component label = tabs.getTabLabel(selectedInfo);
      if (label == null || !label.isShowing() || label.getParent() == null) {
        return null;
      }
      Rectangle labelBounds = SwingUtilities.convertRectangle(label.getParent(), label.getBounds(), pane);
      int catWidth = catWidth();
      int catHeight = catHeight();
      int x = labelBounds.x + labelBounds.width / 2 - catWidth / 2;
      int y = labelBounds.y - catHeight + JBUI.scale(10);
      x = clamp(x, 0, Math.max(0, pane.getWidth() - catWidth));
      y = clamp(y, 0, Math.max(0, pane.getHeight() - catHeight));
      return new Point(x, y);
    }

    private void moveTo(int x, int y) {
      boolean targetChanged = targetX != x || targetY != y;
      targetX = x;
      targetY = y;
      if (!initialized) {
        currentX = targetX;
        currentY = targetY;
        initialized = true;
        settleAtTarget();
        repaintCatArea();
        return;
      }
      if (isAtTarget()) {
        if (targetChanged) {
          settleAtTarget();
        }
        else if (pose == CatPose.SITTING && !blinkStartTimer.isRunning() && !blinkFinishTimer.isRunning()) {
          scheduleBlink();
        }
        repaintCatArea();
        return;
      }
      stopBlinkTimers();
      if (pose != CatPose.TURNING) {
        pose = nextMovingPose();
      }
      ensureAnimationTimerRunning();
    }

    private void animate() {
      Rectangle oldArea = catArea();
      if (pose == CatPose.TURNING) {
        advanceTurn();
        repaint(oldArea.union(catArea()));
        return;
      }
      double dx = targetX - currentX;
      double dy = targetY - currentY;
      double distance = Math.hypot(dx, dy);
      if (distance <= 0.5d) {
        currentX = targetX;
        currentY = targetY;
        settleAtTarget();
        repaint(oldArea.union(catArea()));
        return;
      }
      int desiredDirection = directionForDelta(dx);
      if (shouldTurn(desiredDirection)) {
        startTurn(desiredDirection);
        repaint(oldArea.union(catArea()));
        return;
      }
      if (desiredDirection != 0) {
        direction = desiredDirection;
      }
      double speed = moveSpeed(distance);
      if (distance <= speed) {
        currentX = targetX;
        currentY = targetY;
        settleAtTarget();
      }
      else {
        currentX += dx / distance * speed;
        currentY += dy / distance * speed;
        pose = nextMovingPose(distance);
        animationFrame++;
      }
      repaint(oldArea.union(catArea()));
    }

    private boolean shouldTurn(int desiredDirection) {
      return desiredDirection != 0 &&
             desiredDirection != direction &&
             currentSprites().hasTurning();
    }

    private void startTurn(int desiredDirection) {
      pose = CatPose.TURNING;
      turnTargetDirection = desiredDirection;
      animationFrame = 0;
      stopBlinkTimers();
      ensureAnimationTimerRunning();
    }

    private void advanceTurn() {
      int desiredDirection = directionForDelta(targetX - currentX);
      if (desiredDirection == direction) {
        animationFrame = 0;
        pose = isAtTarget() ? CatPose.SITTING : nextMovingPose();
        if (pose == CatPose.SITTING) {
          settleAtTarget();
        }
        return;
      }
      if (desiredDirection != 0 && desiredDirection != turnTargetDirection) {
        turnTargetDirection = desiredDirection;
        animationFrame = 0;
        return;
      }
      animationFrame++;
      if (animationFrame < TURN_DURATION_FRAMES) {
        return;
      }
      direction = turnTargetDirection;
      animationFrame = 0;
      pose = isAtTarget() ? CatPose.SITTING : nextMovingPose();
      if (pose == CatPose.SITTING) {
        settleAtTarget();
      }
    }

    private int directionForDelta(double dx) {
      if (Math.abs(dx) < 0.5d) {
        return 0;
      }
      return dx > 0 ? 1 : -1;
    }

    private void settleAtTarget() {
      animationTimer.stop();
      stopBlinkTimers();
      pose = CatPose.SITTING;
      animationFrame = 0;
      scheduleBlink();
    }

    private void scheduleBlink() {
      if (!initialized || !isVisible() || !isAtTarget() || !currentSprites().hasIdle()) {
        return;
      }
      blinkStartTimer.restart();
    }

    private void startBlink() {
      if (!initialized || !isVisible() || !isAtTarget() || !currentSprites().hasIdle()) {
        return;
      }
      Rectangle oldArea = catArea();
      pose = CatPose.IDLE;
      animationFrame = 0;
      repaint(oldArea.union(catArea()));
      blinkFinishTimer.restart();
    }

    private void finishBlink() {
      if (pose != CatPose.IDLE) {
        return;
      }
      Rectangle oldArea = catArea();
      pose = CatPose.SITTING;
      animationFrame = 0;
      repaint(oldArea.union(catArea()));
      scheduleBlink();
    }

    private void ensureAnimationTimerRunning() {
      if (!animationTimer.isRunning()) {
        animationTimer.start();
      }
    }

    private void stopAllTimers() {
      animationTimer.stop();
      stopBlinkTimers();
    }

    private void stopBlinkTimers() {
      blinkStartTimer.stop();
      blinkFinishTimer.stop();
    }

    private boolean isAtTarget() {
      return Math.abs(targetX - currentX) < 0.5d && Math.abs(targetY - currentY) < 0.5d;
    }

    private CatPose nextMovingPose() {
      return nextMovingPose(distanceToTarget());
    }

    private CatPose nextMovingPose(double distance) {
      return distance >= JBUI.scale(RUN_DISTANCE) ? CatPose.RUNNING : CatPose.WALKING;
    }

    private double moveSpeed(double distance) {
      if (distance >= JBUI.scale(RUN_DISTANCE)) {
        return Math.max(1.0d, JBUI.scale((int)RUN_SPEED));
      }
      if (distance <= JBUI.scale(WALK_FINISH_DISTANCE)) {
        return Math.max(1.0d, JBUI.scale((int)WALK_SPEED));
      }
      double progress = (distance - JBUI.scale(WALK_FINISH_DISTANCE)) / (double)JBUI.scale(RUN_DISTANCE - WALK_FINISH_DISTANCE);
      double speed = WALK_SPEED + (RUN_SPEED - WALK_SPEED) * progress;
      return Math.max(1.0d, JBUI.scale((int)Math.round(speed)));
    }

    private double distanceToTarget() {
      return Math.hypot(targetX - currentX, targetY - currentY);
    }

    private void repaintCatArea() {
      repaint(catArea());
    }

    private Rectangle catArea() {
      int padding = JBUI.scale(4);
      return new Rectangle(
        (int)Math.round(currentX) - padding,
        (int)Math.round(currentY) - padding,
        catWidth() + padding * 2,
        catHeight() + padding * 2
      );
    }

    private void dispose() {
      if (disposed) {
        return;
      }
      disposed = true;
      stopAllTimers();
      if (layeredPane != null) {
        layeredPane.remove(this);
        layeredPane.repaint();
        layeredPane = null;
      }
      tabsComponent.removeComponentListener(componentListener);
      tabsComponent.removeHierarchyBoundsListener(hierarchyBoundsListener);
      tabsComponent.removeHierarchyListener(hierarchyListener);
      if (tabsComponent.getClientProperty(CAT_OVERLAY_KEY) == this) {
        tabsComponent.putClientProperty(CAT_OVERLAY_KEY, null);
      }
      lastTabs = null;
    }

    private boolean isDisposed() {
      return disposed;
    }

    private void scheduleTargetRefresh() {
      if (disposed || refreshScheduled || lastTabs == null) {
        return;
      }
      refreshScheduled = true;
      SwingUtilities.invokeLater(() -> {
        refreshScheduled = false;
        if (lastTabs != null) {
          refresh(lastTabs);
        }
      });
    }

    @Override
    public boolean contains(int x, int y) {
      return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
      if (!initialized || !isVisible()) {
        return;
      }
      Graphics2D g2 = (Graphics2D)g.create();
      try {
        g2.translate(Math.round(currentX), Math.round(currentY));
        paintCat(g2);
      }
      finally {
        g2.dispose();
      }
    }

    private void paintCat(Graphics2D g2) {
      int width = catWidth();
      int height = catHeight();
      BufferedImage sprite = currentSprites().spriteFor(pose, animationFrame);
      if (sprite == null) {
        return;
      }

      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      if (shouldMirrorCurrentSprite()) {
        g2.translate(width, 0);
        g2.scale(-1, 1);
      }

      paintSprite(g2, sprite, width, height);
    }

    private boolean shouldMirrorCurrentSprite() {
      if (pose == CatPose.TURNING) {
        return turnTargetDirection > 0;
      }
      return direction < 0;
    }

    private int catWidth() {
      return JBUI.scale(scaleDimension(BASE_CAT_WIDTH, currentScalePercent()));
    }

    private int catHeight() {
      return JBUI.scale(scaleDimension(BASE_CAT_HEIGHT, currentScalePercent()));
    }

    private void paintSprite(Graphics2D g2, BufferedImage sprite, int width, int height) {
      int maxHeight = height - JBUI.scale(2);
      double ratio = maxHeight / (double)sprite.getHeight();
      int drawWidth = Math.min(width, Math.max(1, (int)Math.round(sprite.getWidth() * ratio)));
      int drawHeight = Math.min(maxHeight, Math.max(1, (int)Math.round(sprite.getHeight() * ratio)));
      int x = (width - drawWidth) / 2;
      int y = height - drawHeight;
      g2.setComposite(AlphaComposite.SrcOver);
      g2.drawImage(sprite, x, y, drawWidth, drawHeight, null);
    }
  }

  private enum CatPose {
    WALKING,
    RUNNING,
    TURNING,
    SITTING,
    IDLE
  }

  private record CatSprites(List<BufferedImage> walking,
                            List<BufferedImage> running,
                            List<BufferedImage> idle,
                            BufferedImage turning,
                            BufferedImage sitting) {
    private static CatSprites load(String catId) {
      String normalizedCatId = ActiveTabColorSettingsState.normalizeTabCat(catId);
      return new CatSprites(
        loadFrames(normalizedCatId, "walk", 4),
        loadFrames(normalizedCatId, "run", 6),
        loadFrames(normalizedCatId, "idle", 2),
        loadImage(normalizedCatId, "turn.png"),
        loadImage(normalizedCatId, "sit.png")
      );
    }

    private BufferedImage spriteFor(CatPose pose, int animationFrame) {
      if (pose == CatPose.TURNING && turning != null) {
        return turning;
      }
      if (pose == CatPose.IDLE && !idle.isEmpty()) {
        return idle.get(0);
      }
      if (pose == CatPose.SITTING && sitting != null) {
        return sitting;
      }
      if (pose == CatPose.RUNNING && !running.isEmpty()) {
        return running.get(frameIndex(animationFrame, running.size(), 3));
      }
      if (!walking.isEmpty()) {
        return walking.get(frameIndex(animationFrame, walking.size(), 5));
      }
      return sitting;
    }

    private boolean hasIdle() {
      return !idle.isEmpty();
    }

    private boolean hasTurning() {
      return turning != null;
    }

    private static int frameIndex(int animationFrame, int size, int framesPerSprite) {
      return Math.floorMod(animationFrame / Math.max(1, framesPerSprite), size);
    }

    private static List<BufferedImage> loadFrames(String catId, String prefix, int count) {
      List<BufferedImage> frames = new ArrayList<>();
      for (int i = 1; i <= count; i++) {
        BufferedImage image = loadImage(catId, prefix + "_" + i + ".png");
        if (image != null) {
          frames.add(image);
        }
      }
      return List.copyOf(frames);
    }

    private static BufferedImage loadImage(String catId, String name) {
      String path = "/icons/cat/" + catId + "/" + name;
      try (InputStream inputStream = ActiveTabCatDecorator.class.getResourceAsStream(path)) {
        if (inputStream == null) {
          LOG.warn("Missing tab cat sprite: " + path);
          return null;
        }
        return ImageIO.read(inputStream);
      }
      catch (IOException e) {
        LOG.warn("Failed to load tab cat sprite: " + path, e);
        return null;
      }
    }
  }

  private static CatSprites currentSprites() {
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    String tabCat = state == null ? ActiveTabColorSettingsState.DEFAULT_TAB_CAT : state.tabCat;
    String normalized = ActiveTabColorSettingsState.normalizeTabCat(tabCat);
    return CAT_SPRITES.computeIfAbsent(normalized, CatSprites::load);
  }

  private static int currentScalePercent() {
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState();
    return state == null
           ? ActiveTabColorSettingsState.DEFAULT_TAB_CAT_SCALE_PERCENT
           : ActiveTabColorSettingsState.normalizeTabCatScalePercent(state.tabCatScalePercent);
  }

  private static JLayeredPane findLayeredPane(Component component) {
    Window window = SwingUtilities.getWindowAncestor(component);
    if (window instanceof RootPaneContainer rootPaneContainer) {
      return rootPaneContainer.getLayeredPane();
    }
    return null;
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private static int scaleDimension(int value, int scalePercent) {
    return Math.max(1, (int)Math.round(value * scalePercent / 100.0d));
  }
}
