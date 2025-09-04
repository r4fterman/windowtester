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
package com.windowtester.runtime.swing.locator;

import com.windowtester.internal.swing.UIContextSwing;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.condition.HasFocus;
import com.windowtester.runtime.condition.IUICondition;
import com.windowtester.runtime.condition.IsEnabled;
import com.windowtester.runtime.condition.IsEnabledCondition;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import java.awt.Component;
import java.awt.Point;
import java.io.Serial;
import javax.swing.JComboBox;

/**
 * A locator for JComboBoxes.
 */
public class JComboBoxLocator extends AbstractPathLocator implements IsEnabled, HasFocus {

  @Serial private static final long serialVersionUID = -1258270010418601192L;

  /**
   * Creates a locator for a JComboBox with the specified selection
   *
   * @param itemText the selected item
   */
  public JComboBoxLocator(String itemText) {
    this(itemText, null);
  }

  /**
   * Creates a locator for a JComboBox, can be used to locate an editable combo box.
   *
   * @param parent the locator for the parent
   */
  public JComboBoxLocator(SwingWidgetLocator parent) {
    this(null, parent);
  }

  /**
   * Creates a locator for a JComboBox with the specified selection, and parent locator.
   *
   * @param itemText the selected item
   * @param parent   locator for the parent
   */
  public JComboBoxLocator(String itemText, SwingWidgetLocator parent) {
    this(itemText, UNASSIGNED, parent);
  }

  /**
   * Creates an locator for a JComboBox with the sepcified selection, parent locator and it's
   * relative index in the parent
   *
   * @param itemText the selected item
   * @param index    the relative index
   * @param parent   locator for the parent
   */
  public JComboBoxLocator(String itemText, int index, SwingWidgetLocator parent) {
    this(JComboBox.class, itemText, index, parent);
  }

  /**
   * Creates an locator for a JComboBox with the sepcified selection, parent locator and it's
   * relative index in the parent
   *
   * @param cls      the exact class of the component
   * @param itemText the selected item
   * @param index    the relative index
   * @param parent   locator for the parent
   */
  public JComboBoxLocator(Class<?> cls, String itemText, int index, SwingWidgetLocator parent) {
    super(cls, itemText, index, parent);
  }

  @Override
  protected Component doClick(
      IUIContext ui, int clicks, Component component, Point offset, int modifierMask) {
    return ((UIContextSwing) ui)
        .getDriver()
        .clickComboBox((JComboBox<?>) component, getItemText(), clicks);
  }

  /**
   * Create a condition that tests if the given widget is enabled. Note that this is a convenience
   * method, equivalent to:
   * <code>isEnabled(true)</code>
   */
  public IUICondition isEnabled() {
    return isEnabled(true);
  }

  /**
   * Create a condition that tests if the given widget is enabled.
   *
   * @param expected <code>true</code> if the menu is expected to be enabled, else
   *                 <code>false</code>
   * @see IsEnabledCondition
   */
  public IUICondition isEnabled(boolean expected) {
    return new IsEnabledCondition(this, expected);
  }
}
