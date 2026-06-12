# Active Tab Color

[中文文档](README.zh-CN.md)

Active Tab Color is an IntelliJ Platform-based plugin for customizing editor tab colors. It can style the active tab separately, and it can also apply color rules to tabs whose displayed names match a regular expression.

![Settings](img/settings.png)

## Compatibility

This plugin depends on the IntelliJ Platform module, so it is not limited to IntelliJ IDEA. It is intended to work in JetBrains IDEs based on the IntelliJ Platform, such as IntelliJ IDEA, WebStorm, PyCharm, GoLand, PhpStorm, and similar IDEs, as long as they use the standard editor tab UI.

The plugin has been verified with IntelliJ IDEA Community `2024.1.7` and IntelliJ IDEA `2026.1`. Other JetBrains IDEs should work in principle, but they should be manually checked if you plan to publish the plugin with a broad compatibility claim.

## Features

- Enable or disable editor tab color customization.
- Configure active tab styles:
  - Background color
  - Underline border color
  - Outline border color
- Add multiple tab color rules based on regular expressions.
- Enable, disable, delete, move up, or move down rules.
- Configure background, underline border, and outline border colors independently for each rule.
- The plugin does not change tab appearance by default. Colors are applied only after you manually enable a color item and choose a color.

## Settings

After installing the plugin, open:

```text
Settings > Tools > Active Tab Color
```

## Active Tab Settings

The `Active tab` section only affects the currently selected editor tab.

Each color option has a checkbox:

- Checked: the color option is applied.
- Unchecked: the original IDE or theme appearance is preserved.
- `Clear`: removes the configured color for that option.

For example, if only `Background` is checked, the plugin changes only the active tab background and leaves borders untouched.

## Rules

The `Rules` section applies styles by matching the displayed editor tab name. The regex is matched against names such as:

```text
UserService.java
application.yml
README.md
```

Rules are evaluated from top to bottom. The first matching rule wins.

Example:

```text
.*Service.*
```

This matches tab names containing `Service`, such as `UserService.java` or `OrderService.kt`.

If an active tab also matches a rule:

- Configured active tab color options take priority.
- Active tab options that are not configured can still use the matching rule's colors.

## Rule Editing

In the `Rules` section:

- `Add`: add a new rule.
- `Remove`: delete the selected rule.
- `Up` / `Down`: change rule priority.
- `Enabled`: enable or disable the selected rule.
- `Name`: rule label shown in the rule list.
- `Regex`: regular expression used to match the tab name.

If the regex is invalid, `Apply` shows an error and prevents saving.

## Color Behavior

The plugin only changes editor tabs. It does not affect file colors in the Project View.

The plugin does not change tab text colors. This preserves IDE, theme, and Git status colors for file names, such as added or modified files.

The background color is painted inside the rounded tab area to stay close to the original IDE active tab style, instead of filling the whole tab as a square block.

## Build

Build the plugin locally:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.58.21.jdk/Contents/Home ./gradlew buildPlugin
```

The plugin zip is generated under:

```text
build/distributions/
```

## Test

Run unit tests:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.58.21.jdk/Contents/Home ./gradlew test
```
