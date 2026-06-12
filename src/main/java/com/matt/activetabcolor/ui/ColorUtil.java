package com.matt.activetabcolor.ui;

import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public final class ColorUtil {
  private ColorUtil() {
  }

  public static @Nullable Color fromRgb(@Nullable Integer rgb) {
    return rgb == null ? null : new Color(rgb & 0x00FFFFFF);
  }

  public static @Nullable Integer toRgb(@Nullable Color color) {
    return color == null ? null : color.getRGB() & 0x00FFFFFF;
  }

  public static String toHex(int rgb) {
    return String.format("#%06X", rgb & 0x00FFFFFF);
  }
}
