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
import com.windowtester.runtime.swing.SwingWidgetLocator;
import java.awt.Component;
import java.awt.Point;
import java.io.Serial;
import javax.swing.JList;

/**
 * A locator for JLists.
 */
public class JListLocator extends AbstractPathLocator {

  @Serial private static final long serialVersionUID = -1258270010418601192L;

  /**
   * Create an instance of a locator for JList with the text for the selected item
   *
   * @param itemText the selected item text
   */
  public JListLocator(String itemText) {
    this(itemText, null);
  }

  /**
   * Create an instance of a locator for JList with the text for the selected item, relative to its
   * parent.
   *
   * @param itemText the selected item text
   * @param parent   locator for the parent
   */
  public JListLocator(String itemText, SwingWidgetLocator parent) {
    this(itemText, UNASSIGNED, parent);
  }

  /**
   * Create an instance of a locator for JList with the text for the selected item, relative to its
   * parent and with relative index.
   *
   * @param itemText the selected item text
   * @param index    the relative index to it's parent
   * @param parent   the parent locator
   */
  public JListLocator(String itemText, int index, SwingWidgetLocator parent) {
    this(JList.class, itemText, index, parent);
  }

  /**
   * Create an instance of a locator for JList with the text for the selected item, relative to its
   * parent and with relative index.
   *
   * @param cls      the exact class of the component
   * @param itemText the selected item text
   * @param index    the relative index to it's parent
   * @param parent   the parent locator
   */
  public JListLocator(Class<?> cls, String itemText, int index, SwingWidgetLocator parent) {
    super(cls, itemText, index, parent);
  }

  @Override
  protected Component doClick(
      IUIContext ui, int clicks, Component component, Point offset, int modifierMask) {
    return ((UIContextSwing) ui)
        .getDriver()
        .clickListItem(clicks, (JList<?>) component, getItemText(), modifierMask);
  }
}
