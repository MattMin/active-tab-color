package com.matt.activetabcolor.settings;

import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveTabColorConfigurableTest {
  @Test
  void previewFitsAlreadyScaledBoundsWithoutScalingAgain() throws Exception {
    Class<?> catOption = Class.forName(ActiveTabColorConfigurable.class.getName() + "$CatOption");
    var scaledSize = catOption.getDeclaredMethod("scaledSize", int.class, int.class, int.class, int.class);
    scaledSize.setAccessible(true);
    float originalScale = JBUI.scale(1f);
    try {
      for (float scale : new float[]{1f, 2f}) {
        JBUIScale.setUserScaleFactorForTest(scale);
        int maxWidth = JBUI.scale(52);
        int maxHeight = JBUI.scale(38);
        assertEquals(new Dimension(maxHeight, maxHeight),
          scaledSize.invoke(null, 100, 100, maxWidth, maxHeight));
        assertEquals(new Dimension(maxWidth, maxWidth / 2),
          scaledSize.invoke(null, 200, 100, maxWidth, maxHeight));
        assertEquals(new Dimension(maxWidth, maxHeight),
          scaledSize.invoke(null, 0, 100, maxWidth, maxHeight));
        assertEquals(new Dimension(maxWidth, maxHeight),
          scaledSize.invoke(null, 100, 0, maxWidth, maxHeight));
      }
    }
    finally {
      JBUIScale.setUserScaleFactorForTest(originalScale);
    }
  }
}
