package com.matt.activetabcolor.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.Service.Level;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service(Level.APP)
@State(name = "ActiveTabColorSettings", storages = @Storage("activeTabColor.xml"))
public final class ActiveTabColorSettingsState implements PersistentStateComponent<ActiveTabColorSettingsState.PluginState> {
  private PluginState state = new PluginState();

  public static ActiveTabColorSettingsState getInstance() {
    return ApplicationManager.getApplication().getService(ActiveTabColorSettingsState.class);
  }

  @Override
  public @NotNull PluginState getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull PluginState state) {
    this.state = state.copy();
  }

  public void setState(@NotNull PluginState state) {
    this.state = state.copy();
  }

  public static final class PluginState {
    public boolean enabled = true;
    public ColorSettings active = new ColorSettings();
    public List<TabColorRule> rules = new ArrayList<>();

    public PluginState copy() {
      PluginState copy = new PluginState();
      copy.enabled = enabled;
      copy.active = active == null ? new ColorSettings() : active.copy();
      copy.rules = new ArrayList<>();
      if (rules != null) {
        for (TabColorRule rule : rules) {
          copy.rules.add(rule.copy());
        }
      }
      return copy;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PluginState that)) {
        return false;
      }
      return enabled == that.enabled &&
             Objects.equals(active, that.active) &&
             Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
      return Objects.hash(enabled, active, rules);
    }
  }

  public static class ColorSettings {
    public @Nullable Integer backgroundRgb;
    public @Nullable Integer underlineBorderRgb;
    public @Nullable Integer outlineBorderRgb;

    public ColorSettings copy() {
      ColorSettings copy = new ColorSettings();
      copy.backgroundRgb = normalizeRgb(backgroundRgb);
      copy.underlineBorderRgb = normalizeRgb(underlineBorderRgb);
      copy.outlineBorderRgb = normalizeRgb(outlineBorderRgb);
      return copy;
    }

    public boolean isEmpty() {
      return backgroundRgb == null &&
             underlineBorderRgb == null &&
             outlineBorderRgb == null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ColorSettings that)) {
        return false;
      }
      return Objects.equals(backgroundRgb, that.backgroundRgb) &&
             Objects.equals(underlineBorderRgb, that.underlineBorderRgb) &&
             Objects.equals(outlineBorderRgb, that.outlineBorderRgb);
    }

    @Override
    public int hashCode() {
      return Objects.hash(backgroundRgb, underlineBorderRgb, outlineBorderRgb);
    }
  }

  public static final class TabColorRule extends ColorSettings {
    public String name = "New Rule";
    public boolean enabled = true;
    public String pattern = "";

    @Override
    public TabColorRule copy() {
      TabColorRule copy = new TabColorRule();
      copy.name = name == null ? "" : name;
      copy.enabled = enabled;
      copy.pattern = pattern == null ? "" : pattern;
      copy.backgroundRgb = normalizeRgb(backgroundRgb);
      copy.underlineBorderRgb = normalizeRgb(underlineBorderRgb);
      copy.outlineBorderRgb = normalizeRgb(outlineBorderRgb);
      return copy;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof TabColorRule that)) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      return enabled == that.enabled &&
             Objects.equals(name, that.name) &&
             Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), name, enabled, pattern);
    }
  }

  public static @Nullable Integer normalizeRgb(@Nullable Integer rgb) {
    return rgb == null ? null : rgb & 0x00FFFFFF;
  }
}
