package com.matt.activetabcolor.model;

import com.matt.activetabcolor.settings.ActiveTabColorSettingsState;
import org.jetbrains.annotations.Nullable;

public record TabStyle(@Nullable Integer backgroundRgb,
                       @Nullable Integer underlineBorderRgb,
                       @Nullable Integer outlineBorderRgb,
                       int outlineBorderWidth) {
  public static final TabStyle EMPTY = new TabStyle(null, null, null, 1);

  public TabStyle {
    backgroundRgb = normalize(backgroundRgb);
    underlineBorderRgb = normalize(underlineBorderRgb);
    outlineBorderRgb = normalize(outlineBorderRgb);
    outlineBorderWidth = ActiveTabColorSettingsState.normalizeOutlineBorderWidth(outlineBorderWidth);
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
      preferred.outlineBorderRgb != null ? preferred.outlineBorderRgb : outlineBorderRgb,
      preferred.outlineBorderRgb != null ? preferred.outlineBorderWidth : outlineBorderWidth
    );
  }

  private static @Nullable Integer normalize(@Nullable Integer rgb) {
    return rgb == null ? null : rgb & 0x00FFFFFF;
  }
}
