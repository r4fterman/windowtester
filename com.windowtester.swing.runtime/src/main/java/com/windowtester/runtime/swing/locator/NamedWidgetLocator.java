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

import com.windowtester.internal.swing.matcher.NameMatcher;
import com.windowtester.runtime.InaccessibleWidgetException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.HasText;
import com.windowtester.runtime.condition.HasTextCondition;
import com.windowtester.runtime.condition.IUICondition;
import com.windowtester.runtime.condition.IsEnabled;
import com.windowtester.runtime.condition.IsEnabledCondition;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import com.windowtester.runtime.util.StringComparator;
import java.awt.Component;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A locator for components that have a name. Name is set by using the setName() method of the
 * component.
 * <p>
 * The NamedWidgetLocator can be used for all components. Examples of use: For components such as
 * button or text fields,
 * <p>
 * ui.click(new NamedWidgetLocator("city"));
 * <p>
 * For components such as trees, lists, combo boxes
 * <p>
 * ui.click(new JTreeItemLocator("path to node",new NamedWidgetLocator("treeName"))); ui.click(new
 * JListLocator("list element",new NamedWidgetLocator("listName"))); ui.click(new
 * JComboBoxLocator("selection item",new NamedWidgetLocator("comboName")));
 * <p>
 * For a table
 * <p>
 * ui.click(new JTableItemLocator(new Point(1,1),new NamedWidgetLocator("tableName")));
 */
public class NamedWidgetLocator extends SwingWidgetLocator implements HasText, IsEnabled {

  @Serial private static final long serialVersionUID = -6974445702753299953L;

  /**
   * Create a locator instance.
   *
   * @param name the name to match
   */
  public NamedWidgetLocator(String name) {
    this(Component.class, name);
  }

  /**
   * Create a locator instance.
   *
   * @param cls  the class of the widget
   * @param name the name to match
   */
  public NamedWidgetLocator(Class<?> cls, String name) {
    super(cls, name);
    matcher = NameMatcher.create(getNameOrLabel());
  }

  @Override
  protected String getWidgetLocatorStringName() {
    return "NamedWidgetLocator";
  }

  @Override
  protected String getWidgetText(Component widget) throws WidgetSearchException {
    // Find the method
    Method method;
    try {
      method = widget.getClass().getMethod("getText");
    } catch (SecurityException | NoSuchMethodException e) {
      throw new InaccessibleWidgetException(e);
    }

    // Invoke the method and return the result
    try {
      return (String) method.invoke(widget);
    } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
      throw new InaccessibleWidgetException(e);
    }
  }

  /**
   * Create a condition that tests if the given widget has the expected text.
   *
   * @param expected the expected text (can be a regular expression as described in the
   *                 {@link StringComparator} utility)
   */
  public IUICondition hasText(String expected) {
    return new HasTextCondition(this, expected);
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
