# Active Tab Color

[English](README.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Active Tab Color 是一个 IntelliJ-based 插件，用于自定义编辑器 tab 的颜色。它既可以为当前选中的 active tab 设置专属样式，也可以通过正则规则为符合名称的 tab 批量设置样式。

![Settings](img/settings.png)

## 兼容性

这个插件依赖 IntelliJ Platform 模块，因此不只限于 IntelliJ IDEA。理论上，它可以用于基于 IntelliJ Platform 的 JetBrains IDE，例如 IntelliJ IDEA、WebStorm、PyCharm、GoLand、PhpStorm 等，只要对应 IDE 使用标准的编辑器 tab UI。

插件已在 IntelliJ IDEA Community `2024.1.7` 和 IntelliJ IDEA `2026.1` 中验证可用。其他 JetBrains IDE 原理上可以使用，但如果需要发布并声明更宽的兼容范围，建议再针对目标 IDE 做手动验证或增加 verifier 配置。

## 功能

- 启用或停用 tab 颜色自定义。
- 为 active tab 单独配置：
  - 背景色
  - 下划线边框色
  - 外框色
- 添加多条 tab 配色规则，通过正则匹配 tab 上显示的文件名。
- 规则支持启用/停用、删除、上移、下移。
- 每条规则可以单独配置背景色、下划线边框色、外框色。
- 插件首次启用时不会修改任何 tab 样式，只有手动勾选并选择颜色后才会生效。

## 设置入口

安装插件后，在 IDE 中打开：

```text
Settings > Tools > Active Tab Color
```

进入配置页面。

## Active Tab 配置

`Active tab` 区域只影响当前选中的编辑器 tab。

每个颜色项左侧都有一个勾选框：

- 勾选后，该颜色项生效。
- 不勾选时，该颜色项保持 IDE 或当前主题的原始显示。
- 点击 `Clear` 可以清除该颜色项。

例如，只勾选 `Background` 时，插件只修改 active tab 的背景色，不会修改边框颜色。

## Rules 配置

`Rules` 区域用于按 tab 名称匹配并应用颜色。正则匹配的是编辑器 tab 上实际显示的名称，例如：

```text
UserService.java
application.yml
README.md
```

规则按列表从上到下匹配，第一条命中的规则生效。

示例：

```text
.*Service.*
```

这个规则会匹配所有 tab 名称中包含 `Service` 的文件，例如 `UserService.java`、`OrderService.kt`。

如果一个 active tab 同时命中了 active 配置和规则配置：

- active tab 中已勾选的颜色项优先。
- active tab 中未勾选的颜色项可以继续使用命中的规则颜色。

## 规则编辑

在 Rules 区域可以进行这些操作：

- `Add`：添加一条新规则。
- `Remove`：删除当前选中的规则。
- `Up` / `Down`：调整规则优先级。
- `Enabled`：启用或停用当前规则。
- `Name`：规则名称，只用于列表展示。
- `Regex`：用于匹配 tab 名称的正则表达式。

如果正则表达式非法，点击 `Apply` 时会提示错误，并阻止保存。

## 颜色显示说明

插件只修改编辑器 tab，不影响 Project View 中的文件颜色。

插件不会修改 tab 文本颜色。这样可以保留 IDE、主题插件以及 Git 状态带来的文件名颜色变化，例如新增文件、已修改文件等。

背景色会绘制在 tab 内部的圆角区域中，尽量贴近 IDE 原本的 active tab 显示效果，避免整块方形填充。

## 构建

本地构建插件：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.58.21.jdk/Contents/Home ./gradlew buildPlugin
```

构建产物会生成在：

```text
build/distributions/
```

## 测试

运行单元测试：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.58.21.jdk/Contents/Home ./gradlew test
```

## 开源许可证

本项目使用 [MIT License](LICENSE) 开源。
