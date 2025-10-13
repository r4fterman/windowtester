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
package com.windowtester.internal.tester.swing;

import abbot.i18n.Strings;
import abbot.tester.ComponentLocation;
import abbot.tester.JTreeLocation;
import abbot.tester.LocationUnavailableException;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/***
 *  Copy of abbot JTreeTester
 *  Added support for multiple selection for JTree
 */
public class JTreeTester extends abbot.tester.JTreeTester {

  /**
   * Select the given path, expanding parent nodes if necessary.
   */
  public void actionSelectPath(int clickCount, Component component, TreePath path, int buttons) {
    actionSelectRow(clickCount, component, new JTreeLocation(path), buttons);
  }

  /**
   * Select the given row.  If the row is already selected, does nothing.
   */
  public void actionSelectRow(
      int clickCount, Component component, ComponentLocation location, int buttons) {
    var tree = (JTree) component;
    if (location instanceof JTreeLocation jTreeLocation) {
      var path = jTreeLocation.getPath((JTree) component);
      if (path == null) {
        var msg = Strings.get("tester.JTree.path_not_found", new Object[] {location});
        throw new LocationUnavailableException(msg);
      }
      makeVisible(component, path);
    }

    var where = location.getPoint(component);
    var row = tree.getRowForLocation(where.x, where.y);
    if (tree.getLeadSelectionRow() != row || tree.getSelectionCount() != 1) {
      // NOTE: the row bounds *do not* include the expansion handle
      var rect = tree.getRowBounds(row);
      // NOTE: if there's no icon, this may start editing
      actionClick(tree, rect.x + 1, rect.y + rect.height / 2, buttons, clickCount);
    }
  }
}
