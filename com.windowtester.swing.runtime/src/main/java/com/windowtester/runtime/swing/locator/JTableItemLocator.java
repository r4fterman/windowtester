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
import com.windowtester.internal.swing.matcher.NameOrTextMatcher;
import com.windowtester.runtime.IClickDescription;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.IUICondition;
import com.windowtester.runtime.condition.IsSelected;
import com.windowtester.runtime.condition.IsSelectedCondition;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetMatcher;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.locator.WidgetReference;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import java.awt.Component;
import java.awt.Point;
import java.io.Serial;
import javax.swing.JTable;

/**
 * A locator for JTable items.
 */
public class JTableItemLocator extends SwingWidgetLocator implements IsSelected {

  @Serial private static final long serialVersionUID = 7291989565140551064L;

  private final Point rowColumn;

  /**
   * Creates an instance of a locator to an item in a JTable
   *
   * @param rowCol the (row,col) of the item
   */
  public JTableItemLocator(Point rowCol) {
    this(rowCol, null);
  }

  /**
   * Creates an instance of a locator to an item in a JTable
   *
   * @param rowCol    the (row,col) of the item
   * @param tableName table name or <code>null</code> if not set
   */
  public JTableItemLocator(Point rowCol, String tableName) {
    this(rowCol, tableName, null);
  }

  /**
   * Creates an instance of a locator to an item in a JTable, relative to the parent of the JTable.
   *
   * @param rowCol    the (row,col) of the item
   * @param tableName table name or <code>null</code> if not set
   * @param parent    locator of the parent
   */
  public JTableItemLocator(Point rowCol, String tableName, SwingWidgetLocator parent) {
    this(rowCol, tableName, UNASSIGNED, parent);
  }

  /**
   * Creates an instance of a locator to an item in a JTable, relative to the index and parent of
   * the JTable
   *
   * @param rowCol    the (row,col) of the item
   * @param tableName table name or <code>null</code> if not set
   * @param index     the index of the JTable relative to the parent
   * @param parent    locator of the parent
   */
  public JTableItemLocator(Point rowCol, String tableName, int index, SwingWidgetLocator parent) {
    this(JTable.class, rowCol, tableName, index, parent);
  }

  /**
   * Creates an instance of a locator to an item in a JTable, relative to the index and parent of
   * the JTable
   *
   * @param cls       the exact Class of the table
   * @param rowColum  the (row,col) of the item
   * @param tableName table name or <code>null</code> if not set
   * @param index     the index of the JTable relative to the parent
   * @param parent    locator of the parent
   */
  public JTableItemLocator(
      Class<?> cls, Point rowColum, String tableName, int index, SwingWidgetLocator parent) {
    super(cls, tableName, index, parent);
    rowColumn = rowColum;
    matcher = createMatcher(cls, tableName, index, parent);
  }

  private IWidgetMatcher<?> createMatcher(
      Class<?> cls, String tableName, int index, SwingWidgetLocator parent) {
    IWidgetMatcher<?> matcher = null;
    if (cls != null) {
      matcher = new ExactClassMatcher(cls);
    }
    if (tableName != null && !tableName.isEmpty()) {
      matcher = new CompoundMatcher(matcher, NameOrTextMatcher.create(tableName));
    }
    if (index != UNASSIGNED) {
      matcher = IndexMatcher.create(matcher, index);
    }
    if (parent instanceof NamedWidgetLocator) {
      matcher = new CompoundMatcher(matcher, NameMatcher.create(parent.getNameOrLabel()));
      if (index != UNASSIGNED) {
        matcher = HierarchyMatcher.create(matcher, parent.getMatcher(), index);
      }
      return HierarchyMatcher.create(matcher, parent.getMatcher());
    }

    if (matcher != null) {
      return matcher;
    }

    var msg =
        String.format(
            "Unable to create matcher for class=%s, index=%d, parent=%s", cls, index, parent);
    throw new IllegalArgumentException(msg);
  }

  public int getRow() {
    return rowColumn.x;
  }

  public int getColumn() {
    return rowColumn.y;
  }

  @Override
  protected Component doClick(
      IUIContext ui, int clicks, Component component, Point offset, int modifierMask) {
    return ((UIContextSwing) ui)
        .getDriver()
        .clickTable(clicks, (JTable) component, getRow(), getColumn(), modifierMask);
  }

  @Override
  public IWidgetLocator contextClick(
      IUIContext ui, IWidgetReference widget, IClickDescription click, String menuItemPath) {
    var component = (Component) widget.getWidget();
    var clicked =
        ((UIContextSwing) ui)
            .getDriver()
            .contextClickTable((JTable) component, getRow(), getColumn(), menuItemPath);
    return WidgetReference.create(clicked, this);
  }

  @Override
  public boolean isSelected(IUIContext ui) throws WidgetSearchException {
    var table = (JTable) ((IWidgetReference) ui.find(this)).getWidget();
    return table.isCellSelected(getRow(), getColumn());
  }

  /**
   * Create a condition that tests if the given button is selected. Note that this is a convenience
   * method, equivalent to:
   * <code>isSelected(true)</code>
   */
  public IUICondition isSelected() {
    return isSelected(true);
  }

  /**
   * Create a condition that tests if the given button is selected.
   *
   * @param expected <code>true</code> if the button is expected to be selected, else
   *                 <code>false</code>
   */
  public IUICondition isSelected(boolean expected) {
    return new IsSelectedCondition(this, expected);
  }
}
