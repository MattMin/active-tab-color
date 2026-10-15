package com.matt.activetabcolor.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ActiveTabColorDecoratorTest {
  @Test
  void dotPaintsWithoutAnyConfiguredColorsAndCanBeHidden() {
    var visible = paint(null, 1, true);
    var hidden = paint(null, 1, false);
    assertTrue(opaquePixels(visible) > 0);
    assertEquals(0, opaquePixels(hidden));
    assertNotEquals(0, visible.getRGB(7, 7) >>> 24);
    assertEquals(0, visible.getRGB(0, 0) >>> 24);
  }

  @Test
  void thickerOutlinePaintsMorePixelsAndStaysInsideTab() {
    var thin = paint(0x112233, 1, false);
    var thick = paint(0x112233, 6, false);
    assertTrue(opaquePixels(thick) > opaquePixels(thin));
    for (int x = 0; x < thick.getWidth(); x++) {
      assertEquals(0, thick.getRGB(x, 0) >>> 24);
      assertEquals(0, thick.getRGB(x, thick.getHeight() - 1) >>> 24);
    }
    for (int y = 0; y < thick.getHeight(); y++) {
      assertEquals(0, thick.getRGB(0, y) >>> 24);
      assertEquals(0, thick.getRGB(thick.getWidth() - 1, y) >>> 24);
    }
  }

  private static BufferedImage paint(Integer outline, int width, boolean dot) {
    var image = new BufferedImage(120, 30, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      new ActiveTabColorDecorator.ActiveTabOverlayBorder(null, null, null, outline, width, dot)
        .paintBorder(new JPanel(), graphics, 0, 0, image.getWidth(), image.getHeight());
    }
    finally {
      graphics.dispose();
    }
    return image;
  }

  private static int opaquePixels(BufferedImage image) {
    int count = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) != 0) count++;
      }
    }
    return count;
  }
}
