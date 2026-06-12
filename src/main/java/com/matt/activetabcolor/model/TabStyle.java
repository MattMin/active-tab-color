package com.matt.activetabcolor.model;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class TabStyle {
  public static final TabStyle EMPTY = new TabStyle(null, null, null);

  private final @Nullable Integer backgroundRgb;
  private final @Nullable Integer underlineBorderRgb;
  private final @Nullable Integer outlineBorderRgb;

  public TabStyle(@Nullable Integer backgroundRgb,
                  @Nullable Integer underlineBorderRgb,
                  @Nullable Integer outlineBorderRgb) {
    this.backgroundRgb = normalize(backgroundRgb);
    this.underlineBorderRgb = normalize(underlineBorderRgb);
    this.outlineBorderRgb = normalize(outlineBorderRgb);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TabStyle)) return false;
    TabStyle tabStyle = (TabStyle)o;
    return Objects.equals(backgroundRgb, tabStyle.backgroundRgb) &&
           Objects.equals(underlineBorderRgb, tabStyle.underlineBorderRgb) &&
           Objects.equals(outlineBorderRgb, tabStyle.outlineBorderRgb);
  }

  @Override
  public int hashCode() {
    return Objects.hash(backgroundRgb, underlineBorderRgb, outlineBorderRgb);
  }
}
