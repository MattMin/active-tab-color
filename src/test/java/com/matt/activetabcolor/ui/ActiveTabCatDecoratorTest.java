package com.matt.activetabcolor.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tabs.JBTabs;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ActiveTabCatDecoratorTest {
  @Test
  void selectedLabelLayoutEventsScheduleRefreshAndOldLabelIsDetached() throws Exception {
    AtomicInteger tabAccesses = new AtomicInteger();
    SwingUtilities.invokeAndWait(() -> {
      Disposable owner = new ActiveTabCatDecorator();
      try {
        JComponent overlay = createOverlay(new JPanel(), owner);
        JPanel first = new JPanel();
        JPanel second = new JPanel();
        observeLabel(overlay, first);
        setField(overlay, "lastTabs", Proxy.newProxyInstance(JBTabs.class.getClassLoader(),
          new Class<?>[]{JBTabs.class}, (proxy, method, args) -> {
            tabAccesses.incrementAndGet();
            return null;
          }));

        for (int event : new int[]{ComponentEvent.COMPONENT_MOVED, ComponentEvent.COMPONENT_RESIZED,
                                  ComponentEvent.COMPONENT_SHOWN, ComponentEvent.COMPONENT_HIDDEN}) {
          setField(overlay, "refreshScheduled", false);
          first.dispatchEvent(new ComponentEvent(first, event));
          assertEquals(true, getField(overlay, "refreshScheduled"));
        }

        observeLabel(overlay, second);
        assertEquals(0, first.getComponentListeners().length);
        assertEquals(1, second.getComponentListeners().length);
        setField(overlay, "refreshScheduled", false);
        first.dispatchEvent(new ComponentEvent(first, ComponentEvent.COMPONENT_MOVED));
        assertEquals(false, getField(overlay, "refreshScheduled"));
        second.dispatchEvent(new ComponentEvent(second, ComponentEvent.COMPONENT_MOVED));
        assertEquals(true, getField(overlay, "refreshScheduled"));
      }
      finally {
        Disposer.dispose(owner);
      }
    });
    // Drain queued refreshes: none may access tabs after disposal.
    SwingUtilities.invokeAndWait(() -> {});
    assertEquals(0, tabAccesses.get());
  }

  @Test
  void disposingServiceStopsTimersAndRemovesOverlayAndListeners() throws Exception {
    ActiveTabCatDecorator owner = new ActiveTabCatDecorator();
    JPanel tabs = new JPanel();
    JPanel label = new JPanel();
    JLayeredPane pane = new JLayeredPane();
    JComponent[] overlay = new JComponent[1];
    try {
      SwingUtilities.invokeAndWait(() -> {
        overlay[0] = createOverlay(tabs, owner);
        tabs.putClientProperty("activeTabColor.catOverlay", overlay[0]);
        pane.add(overlay[0], JLayeredPane.POPUP_LAYER);
        setField(overlay[0], "layeredPane", pane);
        observeLabel(overlay[0], label);
        for (String timerName : new String[]{"animationTimer", "blinkStartTimer", "blinkFinishTimer"}) {
          Timer timer = (Timer)getField(overlay[0], timerName);
          timer.setInitialDelay(60_000);
          timer.start();
          assertTrue(timer.isRunning());
        }
      });

      Disposer.dispose(owner);

      SwingUtilities.invokeAndWait(() -> {
        assertEquals(true, getField(overlay[0], "disposed"));
        assertEquals(true, getField(owner, "disposed"));
        assertNull(overlay[0].getParent());
        assertNull(tabs.getClientProperty("activeTabColor.catOverlay"));
        assertEquals(0, tabs.getComponentListeners().length);
        assertEquals(0, tabs.getHierarchyBoundsListeners().length);
        assertEquals(0, tabs.getHierarchyListeners().length);
        assertEquals(0, label.getComponentListeners().length);
        for (String timerName : new String[]{"animationTimer", "blinkStartTimer", "blinkFinishTimer"}) {
          assertFalse(((Timer)getField(overlay[0], timerName)).isRunning());
        }
      });
    }
    finally {
      Disposer.dispose(owner);
    }
  }

  private static JComponent createOverlay(JComponent tabs, Disposable owner) {
    try {
      Class<?> type = Class.forName(ActiveTabCatDecorator.class.getName() + "$CatOverlay");
      var constructor = type.getDeclaredConstructor(JComponent.class, Disposable.class);
      constructor.setAccessible(true);
      return (JComponent)constructor.newInstance(tabs, owner);
    }
    catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static void observeLabel(JComponent overlay, Component label) {
    try {
      var method = overlay.getClass().getDeclaredMethod("observeSelectedLabel", Component.class);
      method.setAccessible(true);
      method.invoke(overlay, label);
    }
    catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static Object getField(Object object, String name) {
    try {
      var field = object.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(object);
    }
    catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static void setField(Object object, String name, Object value) {
    try {
      var field = object.getClass().getDeclaredField(name);
      field.setAccessible(true);
      field.set(object, value);
    }
    catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }
}
