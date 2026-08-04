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
package com.windowtester.internal.swing;

import com.windowtester.internal.swing.locator.IWidgetIdentifierStrategy;
import com.windowtester.internal.swing.locator.ScopedComponentIdentifierBuilder;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;

/**
 * A service/factory class that performs various widget querying services and performs identifying
 * widget info inference.
 * <br>
 * Note: instances cache results of calculations.  If the hierarchy changes between uses, results
 * may be invalid.  In cases where the hierarchy is changing, a new instance must be created.
 */
public class WidgetLocatorService {

  private final IWidgetIdentifierStrategy widgetIdentifier = new ScopedComponentIdentifierBuilder();

  /**
   * Get this widget's index relative to its parent widget.
   * <p>Note that indexes only matter in the case where there is at least one sibling
   * that matches the target widget exactly (by class and name/label).  Other cases return -1.
   *
   * @param w      - the widget
   * @param parent - the parent widget
   * @return an index, or -1 if is the only child
   * FIXME: return 0 in only-child case
   */
  public int getIndex(Component w, Component parent) {
    List<Component> children = getChildren(parent, w.getClass());
    // the match counter
    int count = 0;
    // the index of our target widget
    int index = -1;
    // only child case...
    if (children.size() == 1) {
      return index;
    }
    for (Component child : children) {
      // using exact matches...
      if (child.getClass().isAssignableFrom(w.getClass())
          && w.getClass().isAssignableFrom(child.getClass())) {
        // also check for nameOrLabelMatch
        if (nameAndOrLabelDataMatch(w, child)) {
          ++count;
        }
      }
      if (child == w) {
        // indexes are zero-indexed
        index = count - 1;
      }
    }
    return (count > 1) ? index : -1;
  }

  /**
   * Checks to see that widget names/labels match.
   *
   * @param component1 - the first widget
   * @param component2 - the second widget
   * @return true if they match
   */
  private boolean nameAndOrLabelDataMatch(Component component1, Component component2) {
    var text1 = getWidgetText(component1);
    var text2 = getWidgetText(component2);
    if (text1 == null) {
      return text2 == null;
    }
    return text1.equals(text2);
  }

  /**
   * Get the children (of a particular class) of a given parent widget.
   *
   * @param parent - the parent widget
   * @param cls    - the class of child widgets of interest
   * @return a list of children
   */
  public List<Component> getChildren(Component parent, Class<?> cls) {
    var children = new ArrayList<Component>();
    if (parent instanceof Container container) {
      var components = container.getComponents();
      addCheck(children, Arrays.asList(components));
    }

    // prune non-exact class matches
    var pruned = new ArrayList<Component>();
    for (Component child : children) {
      var childClass = child.getClass();
      if (cls.isAssignableFrom(childClass) && childClass.isAssignableFrom(cls)) {
        pruned.add(child);
      }
    }
    return pruned;
  }

  /**
   * Add the contents of this collection to this other collection only if it is non-empty.
   *
   * @param dest - the destination collection
   * @param src  - the source collection
   */
  private void addCheck(Collection<Component> dest, Collection<Component> src) {
    if (!src.isEmpty()) {
      dest.addAll(src);
    }
  }

  /**
   * Extract the text from the given widget.
   *
   * @param component - the widget in question
   * @return the widget's text
   */
  public String getWidgetText(Component component) {
    if (component instanceof AbstractButton button && !(component instanceof JMenuItem)) {
      return button.getText();
    }
    if (component instanceof JLabel label) {
      return label.getText();
    }

    return null;
  }

  /**
   * Given a widget, infers the (minimal) WidgetLocator that uniquely identifies the widget.
   *
   * @param component - the target widget
   * @return the identifying WidgetLocator or null if there was an error in identification
   */
  public SwingWidgetLocator inferIdentifyingInfo(Component component) {
    // pulling inference into a separate strategy
    return widgetIdentifier.identify(component);
  }
}
