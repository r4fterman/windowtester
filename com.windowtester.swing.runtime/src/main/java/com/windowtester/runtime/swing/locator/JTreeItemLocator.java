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

import abbot.tester.JTreeLocation;
import com.windowtester.internal.runtime.matcher.CompoundMatcher;
import com.windowtester.internal.swing.UIContextSwing;
import com.windowtester.internal.swing.matcher.NameOrTextMatcher;
import com.windowtester.runtime.IClickDescription;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.IUICondition;
import com.windowtester.runtime.condition.IsSelected;
import com.windowtester.runtime.condition.IsSelectedCondition;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.locator.WidgetReference;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import java.awt.Component;
import java.awt.Point;
import java.io.Serial;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * A locator for a node in a JTree.
 */
public class JTreeItemLocator extends AbstractPathLocator implements IsSelected {

  @Serial private static final long serialVersionUID = -5514291727454535792L;

  /**
   * Creates an instance of a locator to a node in a JTree
   *
   * @param path a string representing the complete path to the node, such as
   *             "Root/Parent1/Child10/grandChild102".
   */
  public JTreeItemLocator(String path) {
    this(path, (String) null);
  }

  /**
   * Creates an instance of a locator to a node in a JTree
   *
   * @param path a string representing the complete path to the node, such as
   *             "Root/Parent1/Child10/grandChild102".
   */
  public JTreeItemLocator(String path, String treeName) {
    this(path, treeName, null);
  }

  /**
   * Creates an instance of a locator to a node in a JTree
   *
   * @param path   a string representing the complete path to the node, such as
   *               "Root/Parent1/Child10/grandChild102".
   * @param parent locator of the parent
   */
  public JTreeItemLocator(String path, SwingWidgetLocator parent) {
    this(path, null, UNASSIGNED, parent);
  }

  /**
   * Creates an instance of a locator to a node in a JTree
   *
   * @param path   a string representing the complete path to the node, such as
   *               "Root/Parent1/Child10/grandChild102".
   * @param parent locator of the parent
   */
  public JTreeItemLocator(String path, String treeName, SwingWidgetLocator parent) {
    this(path, treeName, UNASSIGNED, parent);
  }

  /**
   * Creates an instance of a locator to a node in a JTree
   *
   * @param path   a string representing the complete path to the node, such as
   *               "Root/Parent1/Child10/grandChild102".
   * @param index  index of the tree relative to it's parent
   * @param parent locator to the parent
   */
  public JTreeItemLocator(String path, int index, SwingWidgetLocator parent) {
    // to search in the tree, we need the entire path, not just label of node.
    // so pass path as itemText
    this(JTree.class, path, null, index, parent);
  }

  /**
   * Creates an instance of a locator to a node in a JTree
   *
   * @param path   a string representing the complete path to the node, such as
   *               "Root/Parent1/Child10/grandChild102".
   * @param index  index of the tree relative to it's parent
   * @param parent locator to the parent
   */
  public JTreeItemLocator(String path, String treeName, int index, SwingWidgetLocator parent) {
    // to search in the tree, we need the entire path, not just label of node.
    // so pass path as itemText
    this(JTree.class, path, treeName, index, parent);
  }

  /**
   * Creates an instance of a locator to a node in a JTree
   *
   * @param cls    the exact class of the component
   * @param path   a string representing the complete path to the node, such as
   *               "Root/Parent1/Child10/grandChild102".
   * @param index  index of the tree relative to it's parent
   * @param parent locator to the parent
   */
  public JTreeItemLocator(
      Class<?> cls, String path, String treeName, int index, SwingWidgetLocator parent) {
    super(cls, path, index, parent);
    if (treeName != null && !treeName.isEmpty()) {
      matcher = new CompoundMatcher(matcher, NameOrTextMatcher.create(treeName));
    }
  }

  @Override
  protected String getWidgetLocatorStringName() {
    return "JTreeItemLocator";
  }

  @Override
  protected Component doClick(
      IUIContext ui, int clicks, Component component, Point offset, int modifierMask) {
    if (clicks > 2) {
      throw new UnsupportedOperationException();
    }
    ((UIContextSwing) ui).getDriver().clickTreeItem(clicks, component, getPath(), modifierMask);
    return component;
  }

  @Override
  public IWidgetLocator contextClick(
      IUIContext ui, IWidgetReference widget, IClickDescription click, String menuItemPath) {
    var component = (Component) widget.getWidget();
    var clicked =
        ((UIContextSwing) ui)
            .getDriver()
            .contextClickTree((JTree) component, getPath(), menuItemPath);
    return WidgetReference.create(clicked, this);
  }

  @Override
  public boolean isSelected(IUIContext ui) throws WidgetSearchException {
    var tree = (JTree) ((IWidgetReference) ui.find(this)).getWidget();
    var nodeNames = getPath().split("/");
    var treePath = new TreePath(nodeNames);
    var row = new JTreeLocation(treePath).getRow(tree);
    return tree.isRowSelected(row);
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
