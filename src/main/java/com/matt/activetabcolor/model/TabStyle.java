package com.matt.activetabcolor.model;

import org.jetbrains.annotations.Nullable;

public record TabStyle(@Nullable Integer backgroundRgb,
                       @Nullable Integer underlineBorderRgb,
                       @Nullable Integer outlineBorderRgb) {
  public static final TabStyle EMPTY = new TabStyle(null, null, null);

  public TabStyle {
    backgroundRgb = normalize(backgroundRgb);
    underlineBorderRgb = normalize(underlineBorderRgb);
    outlineBorderRgb = normalize(outlineBorderRgb);
  }

  public @Nullable Integer getBackgroundRgb() {
    return backgroundRgb;
  }

  public @Nullable Integer getUnderlineBorderRgb() {
    return underlineBorderRgb;
  }

  public @Nullable Integer getOutlineBorderRgb() {
    return outlineBorderRgb;
  }

  public boolean isEmpty() {
    return backgroundRgb == null &&
           underlineBorderRgb == null &&
           outlineBorderRgb == null;
  }

  public TabStyle overlay(TabStyle preferred) {
    if (preferred == null || preferred.isEmpty()) {
      return this;
    }
    return new TabStyle(
      preferred.backgroundRgb != null ? preferred.backgroundRgb : backgroundRgb,
      preferred.underlineBorderRgb != null ? preferred.underlineBorderRgb : underlineBorderRgb,
      preferred.outlineBorderRgb != null ? preferred.outlineBorderRgb : outlineBorderRgb
    );
  }

  private static @Nullable Integer normalize(@Nullable Integer rgb) {
    return rgb == null ? null : rgb & 0x00FFFFFF;
  }
}
