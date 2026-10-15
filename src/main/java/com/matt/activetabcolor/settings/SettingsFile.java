package com.matt.activetabcolor.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.matt.activetabcolor.model.TabStyleResolver;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.PatternSyntaxException;

final class SettingsFile {
  private static final String PLUGIN_ID = "com.matt.active-tab-color";
  private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

  private SettingsFile() {
  }

  static ActiveTabColorSettingsState.PluginState read(Path path) throws IOException {
    return decode(Files.readString(path, StandardCharsets.UTF_8));
  }

  static void write(Path path, ActiveTabColorSettingsState.PluginState state) throws IOException {
    String json = encode(state);
    Path target = path.toAbsolutePath();
    Path temporary = Files.createTempFile(target.getParent(), ".active-tab-color-", ".tmp");
    try {
      Files.writeString(temporary, json, StandardCharsets.UTF_8);
      try {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      }
      catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    catch (IOException e) {
      try {
        Files.deleteIfExists(temporary);
      }
      catch (IOException cleanupError) {
        e.addSuppressed(cleanupError);
      }
      throw e;
    }
  }

  static String encode(ActiveTabColorSettingsState.PluginState state) throws IOException {
    JsonObject document = new JsonObject();
    document.addProperty("pluginId", PLUGIN_ID);
    document.addProperty("formatVersion", 1);
    document.add("settings", GSON.toJsonTree(state));
    String json = GSON.toJson(document) + "\n";
    decode(json);
    return json;
  }

  static ActiveTabColorSettingsState.PluginState decode(String json) throws IOException {
    JsonElement element;
    try (JsonReader reader = new JsonReader(new StringReader(json))) {
      reader.setLenient(false);
      element = GSON.getAdapter(JsonElement.class).read(reader);
      if (reader.peek() != JsonToken.END_DOCUMENT) {
        throw new IOException("Invalid JSON: unexpected trailing content.");
      }
    }
    catch (IOException | RuntimeException e) {
      throw new IOException("Invalid JSON configuration file.", e);
    }
    JsonObject document = object(element, "document");
    if (!PLUGIN_ID.equals(string(document, "pluginId"))) {
      throw new IOException("This file is not an Active Tab Color configuration.");
    }
    integer(document, "formatVersion", 1, 1);
    JsonObject settings = object(required(document, "settings"), "settings");
    for (String field : new String[]{"enabled", "activeOverridesRules", "showActiveTabDot", "showTabCat"}) {
      bool(settings, field);
    }
    String cat = string(settings, "tabCat");
    if (!cat.equals("timi") && !cat.equals("siri") && !cat.equals("luna")) {
      throw new IOException("Invalid tabCat: expected timi, siri or luna.");
    }
    integer(settings, "tabCatScalePercent", ActiveTabColorSettingsState.MIN_TAB_CAT_SCALE_PERCENT,
            ActiveTabColorSettingsState.MAX_TAB_CAT_SCALE_PERCENT);
    integer(settings, "tabCatSpeedPercent", ActiveTabColorSettingsState.MIN_TAB_CAT_SPEED_PERCENT,
            ActiveTabColorSettingsState.MAX_TAB_CAT_SPEED_PERCENT);
    colors(object(required(settings, "active"), "active"));
    JsonElement rules = required(settings, "rules");
    if (!rules.isJsonArray()) {
      throw new IOException("Invalid rules: expected an array.");
    }
    for (int i = 0; i < rules.getAsJsonArray().size(); i++) {
      JsonObject rule = object(rules.getAsJsonArray().get(i), "rules[" + i + "]");
      string(rule, "name");
      string(rule, "pattern");
      bool(rule, "enabled");
      colors(rule);
    }
    ActiveTabColorSettingsState.PluginState state = GSON.fromJson(settings, ActiveTabColorSettingsState.PluginState.class);
    try {
      TabStyleResolver.validateRules(state);
    }
    catch (PatternSyntaxException e) {
      throw new IOException("Invalid rule regex: " + e.getDescription() + ".", e);
    }
    return state;
  }

  private static void colors(JsonObject colors) throws IOException {
    for (String field : new String[]{"backgroundRgb", "underlineBorderRgb", "outlineBorderRgb"}) {
      if (!required(colors, field).isJsonNull()) {
        integer(colors, field, 0, 0xFFFFFF);
      }
    }
    integer(colors, "outlineBorderWidth", ActiveTabColorSettingsState.MIN_OUTLINE_BORDER_WIDTH,
            ActiveTabColorSettingsState.MAX_OUTLINE_BORDER_WIDTH);
  }

  private static JsonElement required(JsonObject object, String field) throws IOException {
    if (!object.has(field)) {
      throw new IOException("Missing field: " + field + ".");
    }
    return object.get(field);
  }

  private static JsonObject object(JsonElement value, String field) throws IOException {
    if (value == null || !value.isJsonObject()) {
      throw new IOException("Invalid " + field + ": expected an object.");
    }
    return value.getAsJsonObject();
  }

  private static String string(JsonObject object, String field) throws IOException {
    JsonElement value = required(object, field);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new IOException("Invalid " + field + ": expected a string.");
    }
    return value.getAsString();
  }

  private static void bool(JsonObject object, String field) throws IOException {
    JsonElement value = required(object, field);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
      throw new IOException("Invalid " + field + ": expected a boolean.");
    }
  }

  private static void integer(JsonObject object, String field, int min, int max) throws IOException {
    JsonElement value = required(object, field);
    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
      try {
        int number = value.getAsBigDecimal().intValueExact();
        if (number >= min && number <= max) {
          return;
        }
      }
      catch (ArithmeticException | NumberFormatException ignored) {
        // Report all invalid integer representations with the same field-specific error.
      }
    }
    throw new IOException("Invalid " + field + ": expected an integer from " + min + " to " + max + ".");
  }
}
