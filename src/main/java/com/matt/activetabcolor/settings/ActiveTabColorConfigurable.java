package com.matt.activetabcolor.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ColorPanel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.matt.activetabcolor.model.TabStyleResolver;
import com.matt.activetabcolor.ui.ActiveTabColorRefresh;
import com.matt.activetabcolor.ui.ColorUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

public final class ActiveTabColorConfigurable implements Configurable {
  private JPanel rootPanel;
  private JCheckBox enabledCheckBox;
  private ColorRow activeBackground;
  private ColorRow activeUnderline;
  private ColorRow activeOutline;
  private DefaultListModel<RuleModel> rulesModel;
  private JList<RuleModel> rulesList;
  private JCheckBox ruleEnabled;
  private JTextField ruleName;
  private JTextField rulePattern;
  private ColorRow ruleBackground;
  private ColorRow ruleUnderline;
  private ColorRow ruleOutline;
  private boolean loadingRule;

  @Override
  public @Nls String getDisplayName() {
    return "Active Tab Color";
  }

  @Override
  public @Nullable JComponent createComponent() {
    rootPanel = new JPanel(new BorderLayout());
    rootPanel.setBorder(JBUI.Borders.empty(8));

    enabledCheckBox = new JBCheckBox("Enable tab color customization");
    rootPanel.add(enabledCheckBox, BorderLayout.NORTH);

    JPanel content = new JPanel(new BorderLayout(JBUI.scale(12), 0));
    content.add(createActivePanel(), BorderLayout.NORTH);
    content.add(createRulesPanel(), BorderLayout.CENTER);
    rootPanel.add(content, BorderLayout.CENTER);

    reset();
    return rootPanel;
  }

  @Override
  public boolean isModified() {
    return !Objects.equals(readStateFromUi(), ActiveTabColorSettingsState.getInstance().getState());
  }

  @Override
  public void apply() throws ConfigurationException {
    ActiveTabColorSettingsState.PluginState uiState = readStateFromUi();
    try {
      TabStyleResolver.validateRules(uiState);
    }
    catch (PatternSyntaxException e) {
      throw new ConfigurationException("Invalid rule regex: " + e.getPattern() + " (" + e.getDescription() + ")");
    }
    ActiveTabColorSettingsState.getInstance().setState(uiState);
    ActiveTabColorRefresh.refreshAllOpenProjects();
  }

  @Override
  public void reset() {
    if (rootPanel == null) {
      return;
    }
    ActiveTabColorSettingsState.PluginState state = ActiveTabColorSettingsState.getInstance().getState().copy();
    enabledCheckBox.setSelected(state.enabled);
    activeBackground.setRgb(state.active.backgroundRgb);
    activeUnderline.setRgb(state.active.underlineBorderRgb);
    activeOutline.setRgb(state.active.outlineBorderRgb);

    rulesModel.clear();
    if (state.rules != null) {
      for (ActiveTabColorSettingsState.TabColorRule rule : state.rules) {
        rulesModel.addElement(RuleModel.fromRule(rule));
      }
    }
    if (!rulesModel.isEmpty()) {
      rulesList.setSelectedIndex(0);
    }
    else {
      loadRule(null);
    }
  }

  @Override
  public void disposeUIResources() {
    rootPanel = null;
  }

  private JComponent createActivePanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Active tab"));
    activeBackground = new ColorRow("Background");
    activeUnderline = new ColorRow("Underline border");
    activeOutline = new ColorRow("Outline border");

    addColorRow(panel, activeBackground, 0);
    addColorRow(panel, activeUnderline, 1);
    addColorRow(panel, activeOutline, 2);
    addVerticalFiller(panel, 3);
    return panel;
  }

  private JComponent createRulesPanel() {
    JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), 0));
    panel.setBorder(BorderFactory.createTitledBorder("Rules"));

    rulesModel = new DefaultListModel<>();
    rulesList = new JBList<>(rulesModel);
    rulesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    rulesList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        loadRule(rulesList.getSelectedValue());
      }
    });

    JPanel listPanel = new JPanel(new BorderLayout(0, JBUI.scale(6)));
    listPanel.add(new JBScrollPane(rulesList), BorderLayout.CENTER);
    listPanel.add(createRuleButtons(), BorderLayout.SOUTH);
    panel.add(listPanel, BorderLayout.WEST);
    panel.add(createRuleEditor(), BorderLayout.CENTER);
    return panel;
  }

  private JComponent createRuleButtons() {
    JPanel panel = new JPanel(new GridBagLayout());
    JButton add = new JButton("Add");
    JButton remove = new JButton("Remove");
    JButton up = new JButton("Up");
    JButton down = new JButton("Down");

    add.addActionListener(e -> addRule());
    remove.addActionListener(e -> removeRule());
    up.addActionListener(e -> moveRule(-1));
    down.addActionListener(e -> moveRule(1));

    panel.add(add);
    panel.add(remove);
    panel.add(up);
    panel.add(down);
    return panel;
  }

  private JComponent createRuleEditor() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(JBUI.Borders.emptyLeft(8));

    ruleEnabled = new JBCheckBox("Enabled");
    ruleName = new JBTextField();
    rulePattern = new JBTextField();
    ruleBackground = new ColorRow("Background");
    ruleUnderline = new ColorRow("Underline border");
    ruleOutline = new ColorRow("Outline border");

    ruleEnabled.addActionListener(e -> updateSelectedRule());
    installDocumentListener(ruleName);
    installDocumentListener(rulePattern);
    ruleBackground.setOnChange(this::updateSelectedRule);
    ruleUnderline.setOnChange(this::updateSelectedRule);
    ruleOutline.setOnChange(this::updateSelectedRule);

    GridBagConstraints c = baseConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    panel.add(createRulesHelpLabel(), c);

    c = baseConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    panel.add(ruleEnabled, c);

    addLabeledField(panel, "Name", ruleName, 2);
    addLabeledField(panel, "Regex", rulePattern, 3);
    addColorRow(panel, ruleBackground, 4);
    addColorRow(panel, ruleUnderline, 5);
    addColorRow(panel, ruleOutline, 6);
    addVerticalFiller(panel, 7);
    return panel;
  }

  private JComponent createRulesHelpLabel() {
    JTextArea text = new JTextArea("Rules are evaluated from top to bottom. Regex matches the editor tab name shown in the tab, for example UserService.java.");
    text.setEditable(false);
    text.setFocusable(false);
    text.setOpaque(false);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setRows(2);
    text.setColumns(42);
    text.setBorder(JBUI.Borders.emptyBottom(2));
    return text;
  }

  private void addRule() {
    RuleModel model = new RuleModel();
    model.name = "New Rule";
    model.enabled = true;
    model.pattern = "";
    rulesModel.addElement(model);
    rulesList.setSelectedIndex(rulesModel.size() - 1);
  }

  private void removeRule() {
    int index = rulesList.getSelectedIndex();
    if (index < 0) {
      return;
    }
    rulesModel.remove(index);
    if (!rulesModel.isEmpty()) {
      rulesList.setSelectedIndex(Math.min(index, rulesModel.size() - 1));
    }
    else {
      loadRule(null);
    }
  }

  private void moveRule(int direction) {
    int index = rulesList.getSelectedIndex();
    int nextIndex = index + direction;
    if (index < 0 || nextIndex < 0 || nextIndex >= rulesModel.size()) {
      return;
    }
    RuleModel rule = rulesModel.remove(index);
    rulesModel.add(nextIndex, rule);
    rulesList.setSelectedIndex(nextIndex);
  }

  private void loadRule(RuleModel rule) {
    loadingRule = true;
    boolean enabled = rule != null;
    ruleEnabled.setEnabled(enabled);
    ruleName.setEnabled(enabled);
    rulePattern.setEnabled(enabled);
    ruleBackground.setEnabled(enabled);
    ruleUnderline.setEnabled(enabled);
    ruleOutline.setEnabled(enabled);

    ruleEnabled.setSelected(rule != null && rule.enabled);
    ruleName.setText(rule == null ? "" : rule.name);
    rulePattern.setText(rule == null ? "" : rule.pattern);
    ruleBackground.setRgb(rule == null ? null : rule.backgroundRgb);
    ruleUnderline.setRgb(rule == null ? null : rule.underlineBorderRgb);
    ruleOutline.setRgb(rule == null ? null : rule.outlineBorderRgb);
    loadingRule = false;
  }

  private void updateSelectedRule() {
    if (loadingRule) {
      return;
    }
    RuleModel rule = rulesList.getSelectedValue();
    if (rule == null) {
      return;
    }
    rule.enabled = ruleEnabled.isSelected();
    rule.name = ruleName.getText();
    rule.pattern = rulePattern.getText();
    rule.backgroundRgb = ruleBackground.getRgb();
    rule.underlineBorderRgb = ruleUnderline.getRgb();
    rule.outlineBorderRgb = ruleOutline.getRgb();
    rulesList.repaint();
  }

  private ActiveTabColorSettingsState.PluginState readStateFromUi() {
    ActiveTabColorSettingsState.PluginState state = new ActiveTabColorSettingsState.PluginState();
    state.enabled = enabledCheckBox.isSelected();
    state.active.backgroundRgb = activeBackground.getRgb();
    state.active.underlineBorderRgb = activeUnderline.getRgb();
    state.active.outlineBorderRgb = activeOutline.getRgb();

    for (int i = 0; i < rulesModel.size(); i++) {
      state.rules.add(rulesModel.get(i).toRule());
    }
    return state;
  }

  private void addLabeledField(JPanel panel, String label, JTextField field, int row) {
    GridBagConstraints labelConstraints = baseConstraints();
    labelConstraints.gridx = 0;
    labelConstraints.gridy = row;
    panel.add(new JLabel(label), labelConstraints);

    GridBagConstraints fieldConstraints = baseConstraints();
    fieldConstraints.gridx = 1;
    fieldConstraints.gridy = row;
    fieldConstraints.weightx = 1;
    fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
    panel.add(field, fieldConstraints);
  }

  private void addColorRow(JPanel panel, ColorRow row, int gridRow) {
    GridBagConstraints constraints = baseConstraints();
    constraints.gridx = 0;
    constraints.gridy = gridRow;
    constraints.gridwidth = 2;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0;
    panel.add(row.getPanel(), constraints);
  }

  private void addVerticalFiller(JPanel panel, int row) {
    GridBagConstraints constraints = baseConstraints();
    constraints.gridx = 0;
    constraints.gridy = row;
    constraints.gridwidth = 2;
    constraints.weightx = 1;
    constraints.weighty = 1;
    constraints.fill = GridBagConstraints.BOTH;
    panel.add(new JPanel(), constraints);
  }

  private GridBagConstraints baseConstraints() {
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(2, 4, 2, 4);
    c.anchor = GridBagConstraints.WEST;
    return c;
  }

  private void installDocumentListener(JTextField field) {
    field.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        updateSelectedRule();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        updateSelectedRule();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        updateSelectedRule();
      }
    });
  }

  private static final class RuleModel {
    private String name = "";
    private boolean enabled;
    private String pattern = "";
    private Integer backgroundRgb;
    private Integer underlineBorderRgb;
    private Integer outlineBorderRgb;

    private static RuleModel fromRule(ActiveTabColorSettingsState.TabColorRule rule) {
      RuleModel model = new RuleModel();
      model.name = rule.name;
      model.enabled = rule.enabled;
      model.pattern = rule.pattern;
      model.backgroundRgb = rule.backgroundRgb;
      model.underlineBorderRgb = rule.underlineBorderRgb;
      model.outlineBorderRgb = rule.outlineBorderRgb;
      return model;
    }

    private ActiveTabColorSettingsState.TabColorRule toRule() {
      ActiveTabColorSettingsState.TabColorRule rule = new ActiveTabColorSettingsState.TabColorRule();
      rule.name = name == null ? "" : name;
      rule.enabled = enabled;
      rule.pattern = pattern == null ? "" : pattern;
      rule.backgroundRgb = backgroundRgb;
      rule.underlineBorderRgb = underlineBorderRgb;
      rule.outlineBorderRgb = outlineBorderRgb;
      return rule;
    }

    @Override
    public String toString() {
      String displayName = name == null || name.isBlank() ? "Unnamed rule" : name;
      return (enabled ? "" : "[Disabled] ") + displayName;
    }
  }

  private static final class ColorRow {
    private static final int DEFAULT_RGB = 0x4C9AFF;

    private final JPanel panel = new JPanel(new GridBagLayout());
    private final JCheckBox useCheckBox = new JBCheckBox();
    private final JLabel label;
    private final ColorPanel colorPanel = new ColorPanel();
    private final JButton clearButton = new JButton("Clear");
    private Runnable onChange;

    private ColorRow(String label) {
      this.label = new JLabel(label);
      panel.setMinimumSize(new Dimension(0, 0));
      colorPanel.setEditable(true);
      useCheckBox.addActionListener(e -> {
        if (useCheckBox.isSelected() && colorPanel.getSelectedColor() == null) {
          colorPanel.setSelectedColor(new Color(DEFAULT_RGB));
        }
        updateEnabled();
        notifyChanged();
      });
      colorPanel.addActionListener(e -> {
        if (colorPanel.getSelectedColor() != null) {
          useCheckBox.setSelected(true);
        }
        updateEnabled();
        notifyChanged();
      });
      clearButton.addActionListener(e -> {
        useCheckBox.setSelected(false);
        colorPanel.setSelectedColor(null);
        updateEnabled();
        notifyChanged();
      });

      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(2, 2, 2, 2);
      c.anchor = GridBagConstraints.WEST;
      c.gridx = 0;
      panel.add(useCheckBox, c);
      c.gridx = 1;
      this.label.setPreferredSize(JBUI.size(112, this.label.getPreferredSize().height));
      panel.add(this.label, c);
      c.gridx = 2;
      panel.add(colorPanel, c);
      c.gridx = 3;
      panel.add(clearButton, c);
      c.gridx = 4;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      panel.add(new JPanel(), c);
      updateEnabled();
    }

    private JPanel getPanel() {
      return panel;
    }

    private void setOnChange(Runnable onChange) {
      this.onChange = onChange;
    }

    private void setRgb(Integer rgb) {
      Integer normalized = ActiveTabColorSettingsState.normalizeRgb(rgb);
      useCheckBox.setSelected(normalized != null);
      colorPanel.setSelectedColor(ColorUtil.fromRgb(normalized));
      updateEnabled();
    }

    private Integer getRgb() {
      return useCheckBox.isSelected() ? ColorUtil.toRgb(colorPanel.getSelectedColor()) : null;
    }

    private void setEnabled(boolean enabled) {
      useCheckBox.setEnabled(enabled);
      clearButton.setEnabled(enabled);
      updateEnabled();
    }

    private void updateEnabled() {
      colorPanel.setEnabled(useCheckBox.isEnabled() && useCheckBox.isSelected());
    }

    private void notifyChanged() {
      if (onChange != null) {
        SwingUtilities.invokeLater(onChange);
      }
    }
  }
}
