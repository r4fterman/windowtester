/*******************************************************************************
 *  Copyright (c) 2012 Google, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Google, Inc. - initial API and implementation
 *******************************************************************************/
package com.windowtester.internal.swing.locator;

import com.windowtester.internal.swing.util.ComponentAccessor;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import com.windowtester.runtime.swing.locator.JButtonLocator;
import com.windowtester.runtime.swing.locator.JCheckBoxLocator;
import com.windowtester.runtime.swing.locator.JMenuItemLocator;
import com.windowtester.runtime.swing.locator.JRadioButtonLocator;
import com.windowtester.runtime.swing.locator.JTextComponentLocator;
import com.windowtester.runtime.swing.locator.JToggleButtonLocator;
import com.windowtester.runtime.swing.locator.LabeledTextLocator;
import com.windowtester.runtime.swing.locator.NamedWidgetLocator;
import java.awt.Component;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

/**
 * A factory for creating locators from concrete widgets.
 */
public class WidgetLocatorFactory {

  private static final WidgetLocatorFactory INSTANCE = new WidgetLocatorFactory();

  public static WidgetLocatorFactory getInstance() {
    return INSTANCE;
  }

  private WidgetLocatorFactory() {
    // hide public constructor
  }

  public SwingWidgetLocator create(Component component) {
    var name = component.getName();
    if (name != null && !name.contains("OptionPane")) {
      return new NamedWidgetLocator(component.getClass(), name);
    }

    if (component instanceof JButton button) {
      return new JButtonLocator(button.getText());
    }
    if (component instanceof JRadioButton radioButton) {
      return new JRadioButtonLocator(radioButton.getText());
    }
    if (component instanceof JCheckBox checkBox) {
      return new JCheckBoxLocator(checkBox.getText());
    }
    if (component instanceof JToggleButton toggleButton) {
      return new JToggleButtonLocator(toggleButton.getText());
    }
    if (component instanceof JMenuItem item) {
      return new JMenuItemLocator(component.getClass(), createMenuItemPath(item));
    }
    if (component instanceof JTextField) {
      // create LabeledTextLocator
      var labeledText = getLabeledText(component);
      if (labeledText.isPresent()) {
        return new LabeledTextLocator(labeledText.get());
      }
    }
    // widget locators for all other text components
    if (component instanceof JTextComponent) {
      return new JTextComponentLocator(component.getClass());
    }

    // N.B. tree item locators are built up at codegen time
    return createDefaultLocator(component);
  }

  private String createMenuItemPath(JMenuItem item) {
    var string = ComponentAccessor.extractMenuPath(item);
    var itemLabel = ComponentAccessor.extractMenuItemLabel(item);
    return string + "/" + itemLabel;
  }

  private Optional<String> getLabeledText(Component component) {
    var parent = component.getParent();
    if (parent == null) {
      return Optional.empty();
    }

    return Arrays.stream(parent.getComponents())
        .filter(JLabel.class::isInstance)
        .map(JLabel.class::cast)
        .filter(label -> isCorrectComponent(label, component))
        .map(JLabel::getText)
        .findFirst();
  }

  private boolean isCorrectComponent(JLabel label, Component component) {
    return label.getClass().equals(component.getClass()) && label == component;
  }

  private SwingWidgetLocator createDefaultLocator(Component component) {
    var cls = component.getClass();
    var name = component.getName();

    if (component instanceof AbstractButton button) {
      var text = button.getText();
      if (text != null) {
        return new SwingWidgetLocator(cls, text);
      }
    }

    if (name != null) {
      return new SwingWidgetLocator(cls, name);
    }
    return new SwingWidgetLocator(cls);
  }
}
