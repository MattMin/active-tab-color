package com.matt.activetabcolor.settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsFileTest {
  @TempDir Path directory;

  @Test
  void defaultSettingsRoundTripWithUnsetColors() throws IOException {
    var state = new ActiveTabColorSettingsState.PluginState();
    var restored = SettingsFile.decode(SettingsFile.encode(state));
    assertEquals(state, restored);
    assertNull(restored.active.backgroundRgb);
    assertTrue(restored.rules.isEmpty());
  }

  @Test
  void completeSettingsRoundTripPreservesOrderAndDisabledRules() throws IOException {
    var state = new ActiveTabColorSettingsState.PluginState();
    state.enabled = false;
    state.activeOverridesRules = false;
    state.showActiveTabDot = true;
    state.showTabCat = true;
    state.tabCat = "luna";
    state.tabCatScalePercent = 150;
    state.tabCatSpeedPercent = 50;
    state.active.backgroundRgb = 0;
    state.active.underlineBorderRgb = 0xFFFFFF;
    state.active.outlineBorderRgb = 0x123456;
    state.active.outlineBorderWidth = 6;
    var first = new ActiveTabColorSettingsState.TabColorRule();
    first.name = "中文规则 🐱";
    first.pattern = ".*\\.java$";
    first.backgroundRgb = 0xABCDEF;
    first.outlineBorderWidth = 3;
    var second = new ActiveTabColorSettingsState.TabColorRule();
    second.name = "Disabled";
    second.enabled = false;
    second.pattern = "[";
    state.rules.add(first);
    state.rules.add(second);
    Path file = directory.resolve("settings.json");
    SettingsFile.write(file, state);
    assertEquals(state, SettingsFile.read(file));
    assertTrue(Files.readString(file).contains("中文规则"));
  }

  @Test
  void rejectsInvalidJsonAndNonObjectDocuments() {
    for (String json : new String[]{"", " ", "{", "null", "[]", "{} {}", "{pluginId:'x'}", "{\"x\":NaN}"}) {
      assertThrows(IOException.class, () -> SettingsFile.decode(json), json);
    }
  }

  @Test
  void rejectsWrongIdentityVersionAndMissingSettings() throws IOException {
    reject(doc -> doc.addProperty("pluginId", "other.plugin"), "configuration");
    reject(doc -> doc.addProperty("formatVersion", 2), "formatVersion");
    reject(doc -> doc.addProperty("formatVersion", "1"), "formatVersion");
    reject(doc -> doc.remove("settings"), "settings");
    reject(doc -> doc.add("settings", null), "settings");
  }

  @Test
  void rejectsMissingFieldsAndWrongTypes() throws IOException {
    reject(doc -> settings(doc).remove("enabled"), "enabled");
    reject(doc -> settings(doc).addProperty("enabled", "true"), "enabled");
    reject(doc -> settings(doc).add("showTabCat", null), "showTabCat");
    reject(doc -> settings(doc).addProperty("tabCat", 123), "tabCat");
    reject(doc -> settings(doc).addProperty("tabCat", "unknown"), "tabCat");
    reject(doc -> settings(doc).addProperty("rules", "[]"), "rules");
    reject(doc -> settings(doc).getAsJsonArray("rules").add((JsonObject)null), "rules[0]");
    reject(doc -> settings(doc).getAsJsonObject("active").remove("backgroundRgb"), "backgroundRgb");
    reject(doc -> settings(doc).getAsJsonObject("active").addProperty("backgroundRgb", "#FFFFFF"), "backgroundRgb");
  }

  @Test
  void rejectsOutOfRangeAndNonIntegerValues() throws IOException {
    reject(doc -> settings(doc).addProperty("tabCatScalePercent", 59), "tabCatScalePercent");
    reject(doc -> settings(doc).addProperty("tabCatScalePercent", 151), "tabCatScalePercent");
    reject(doc -> settings(doc).addProperty("tabCatSpeedPercent", 49), "tabCatSpeedPercent");
    reject(doc -> settings(doc).addProperty("tabCatSpeedPercent", 201), "tabCatSpeedPercent");
    reject(doc -> settings(doc).addProperty("tabCatSpeedPercent", 100.5), "tabCatSpeedPercent");
    reject(doc -> settings(doc).addProperty("tabCatSpeedPercent", Long.MAX_VALUE), "tabCatSpeedPercent");
    reject(doc -> settings(doc).getAsJsonObject("active").addProperty("outlineBorderWidth", 0), "outlineBorderWidth");
    reject(doc -> settings(doc).getAsJsonObject("active").addProperty("outlineBorderWidth", 7), "outlineBorderWidth");
    reject(doc -> settings(doc).getAsJsonObject("active").addProperty("backgroundRgb", -1), "backgroundRgb");
    reject(doc -> settings(doc).getAsJsonObject("active").addProperty("backgroundRgb", 0x1000000), "backgroundRgb");
  }

  @Test
  void validatesRuleFieldsAndEnabledRegex() throws IOException {
    var state = new ActiveTabColorSettingsState.PluginState();
    state.rules.add(new ActiveTabColorSettingsState.TabColorRule());
    String json = SettingsFile.encode(state);
    JsonObject doc = JsonParser.parseString(json).getAsJsonObject();
    doc.getAsJsonObject("settings").getAsJsonArray("rules").get(0).getAsJsonObject().addProperty("pattern", "[");
    assertTrue(assertThrows(IOException.class, () -> SettingsFile.decode(doc.toString())).getMessage().contains("regex"));
    doc.getAsJsonObject("settings").getAsJsonArray("rules").get(0).getAsJsonObject().remove("name");
    assertTrue(assertThrows(IOException.class, () -> SettingsFile.decode(doc.toString())).getMessage().contains("name"));
  }

  @Test
  void failedImportDoesNotChangeSavedState() throws IOException {
    var service = new ActiveTabColorSettingsState();
    var original = service.getState().copy();
    Path file = directory.resolve("broken.json");
    Files.writeString(file, "{broken");
    assertThrows(IOException.class, () -> service.setState(SettingsFile.read(file)));
    assertEquals(original, service.getState());
  }

  @Test
  void replacesExistingExportAndCleansTemporaryFilesOnFailure() throws IOException {
    Path file = directory.resolve("settings.json");
    Files.writeString(file, "old content");
    SettingsFile.write(file, new ActiveTabColorSettingsState.PluginState());
    assertNotNull(SettingsFile.read(file));
    Path targetDirectory = Files.createDirectory(directory.resolve("folder"));
    Files.writeString(targetDirectory.resolve("keep.txt"), "keep");
    assertThrows(IOException.class, () -> SettingsFile.write(targetDirectory, new ActiveTabColorSettingsState.PluginState()));
    assertEquals("keep", Files.readString(targetDirectory.resolve("keep.txt")));
    try (var files = Files.list(directory)) {
      assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
    }
  }

  @Test
  void reportsReadAndWriteFailures() {
    assertThrows(IOException.class, () -> SettingsFile.read(directory.resolve("missing.json")));
    assertThrows(IOException.class, () -> SettingsFile.read(directory));
    assertThrows(IOException.class, () -> SettingsFile.write(directory.resolve("missing/settings.json"),
                                                           new ActiveTabColorSettingsState.PluginState()));
  }

  @Test
  void invalidExportDoesNotOverwriteExistingFile() throws IOException {
    Path file = directory.resolve("settings.json");
    Files.writeString(file, "original");
    var state = new ActiveTabColorSettingsState.PluginState();
    state.tabCatScalePercent = 0;
    assertThrows(IOException.class, () -> SettingsFile.write(file, state));
    assertEquals("original", Files.readString(file));
  }

  private static JsonObject settings(JsonObject doc) {
    return doc.getAsJsonObject("settings");
  }

  private static void reject(Consumer<JsonObject> mutation, String message) throws IOException {
    JsonObject doc = JsonParser.parseString(SettingsFile.encode(new ActiveTabColorSettingsState.PluginState())).getAsJsonObject();
    mutation.accept(doc);
    assertTrue(assertThrows(IOException.class, () -> SettingsFile.decode(doc.toString())).getMessage().contains(message));
  }
}
