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

import com.windowtester.internal.runtime.matcher.CompoundMatcher;
import com.windowtester.internal.runtime.matcher.ExactClassMatcher;
import com.windowtester.internal.swing.UIContextSwing;
import com.windowtester.internal.swing.matcher.HierarchyMatcher;
import com.windowtester.internal.swing.matcher.IndexMatcher;
import com.windowtester.internal.swing.matcher.NameMatcher;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.locator.IWidgetMatcher;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import java.awt.Component;
import java.awt.Point;
import java.io.Serial;
import javax.swing.JTabbedPane;

/**
 * A locator for JTabbedPane.
 */
public class JTabbedPaneLocator extends SwingWidgetLocator {

  @Serial private static final long serialVersionUID = 2471285225375825938L;

  /**
   * Creates an instance of a locator for a JTabbedPane
   *
   * @param tabLabel the label of the selected tab
   */
  public JTabbedPaneLocator(String tabLabel) {
    this(tabLabel, null);
  }

  /**
   * Creates an instance of a locator for a JTabbedPane
   *
   * @param tabLabel the label of the selected tab
   * @param parent   locator for the parent
   */
  public JTabbedPaneLocator(String tabLabel, SwingWidgetLocator parent) {
    this(tabLabel, -1, parent);
  }

  /**
   * Creates an instance of a locator for a JTabbedPane
   *
   * @param tabLabel the label of the selected tab
   * @param index    the index of the JTabbedPane relative to it's parent
   * @param parent   locator for the parent
   */
  public JTabbedPaneLocator(String tabLabel, int index, SwingWidgetLocator parent) {
    this(JTabbedPane.class, tabLabel, index, parent);
  }

  public JTabbedPaneLocator(Class<?> cls, String tabLabel, int index, SwingWidgetLocator parent) {
    super(cls, tabLabel, index, parent);
    matcher = createMatcher(cls, index, parent);
  }

  private IWidgetMatcher<?> createMatcher(Class<?> cls, int index, SwingWidgetLocator parent) {
    IWidgetMatcher<?> matcher = new ExactClassMatcher(cls);

    if (index != UNASSIGNED) {
      matcher = IndexMatcher.create(matcher, index);
    }

    if (parent != null) {
      if (parent instanceof NamedWidgetLocator) {
        matcher = new CompoundMatcher(matcher, NameMatcher.create(parent.getNameOrLabel()));
      } else {
        if (index != UNASSIGNED) {
          matcher = HierarchyMatcher.create(matcher, parent.getMatcher(), index);
        } else {
          matcher = HierarchyMatcher.create(matcher, parent.getMatcher());
        }
      }
    }

    return matcher;
  }

  @Override
  protected Component doClick(
      IUIContext ui, int clicks, Component component, Point offset, int modifierMask) {
    return ((UIContextSwing) ui).getDriver().click(component, getNameOrLabel());
  }
}
