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

import abbot.finder.Matcher;
import com.windowtester.internal.swing.locator.IWidgetIdentifierStrategy;
import com.windowtester.internal.swing.locator.MatcherFactory;
import com.windowtester.internal.swing.locator.ScopedComponentIdentifierBuilder;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
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

  //	a list of keys which we want to propagate to locators
  private static final String[] INTERESTING_KEYS = {};

  private final IWidgetIdentifierStrategy widgetIdentifier = new ScopedComponentIdentifierBuilder();

  /**
   * Generate a Matcher that can be used to identify the widget described by this WidgetLocator
   * object.
   *
   * @return a Matcher that matches this object.
   * @see Matcher
   */
  public static Matcher getMatcher(SwingWidgetLocator wl) {
    return MatcherFactory.getMatcher(wl);
  }

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
    var pruned = new ArrayList<Component>();
    if (parent instanceof Container container) {
      for (Component child : container.getComponents()) {
        if (child.getClass() == cls) {
          pruned.add(child);
        }
      }
    }
    return pruned;
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
   * Create an (unelaborated) info object for this widget.
   *
   * @param component - the widget to describe.
   * @return an info object that describes the widget.
   */
  private SwingWidgetLocator getInfo(Component component) {
    if (component == null) {
      // return new WidgetLocator(NullParent.class);
      // TODO: handle NullParent case...
      return null;
    }
    /**
     * CCombos require special treatment as the chevron is a button and receives the click event.
     * Instead of that button, we want to be identifying the combo itself (the button's parent).
     */
    //		if (component instanceof Button) {
    //			Widget parent = new ButtonTester().getParent((Button)component);
    //			if (parent instanceof CCombo)
    //				component = parent;
    //		}

    var cls = component.getClass();
    /**
     * We don't want the combo text to be part of the identifying information since it
     * is only set to the value AFTER it is selected...
     * Text values are also too volatile to use as identifiers.
     *
     */
    var text = getWidgetText(component);
    var locator = getSwingWidgetLocator(text, cls);

    setDataValues(locator, component);
    return locator;
  }

  private static SwingWidgetLocator getSwingWidgetLocator(
      String text, Class<? extends Component> cls) {
    if (text != null) {
      return new SwingWidgetLocator(cls, text);
    }
    return new SwingWidgetLocator(cls);
  }

  private void setDataValues(SwingWidgetLocator locator, Component component) {
    //	propagate values of interest from the widget to the locator
    Object value = null;
    for (String interestingKey : INTERESTING_KEYS) {
      if (component instanceof JComponent jComponent) {
        value = jComponent.getClientProperty(interestingKey);
      }
      if (value != null) {
        locator.setData(interestingKey, value.toString());
      }
    }
  }

  /**
   * Given a widget, infers the (minimal) WidgetLocator that uniquely identifies the widget.
   *
   * @param component - the target widget
   * @return the identifying WidgetLocator or null if there was an error in identification
   */
  public SwingWidgetLocator inferIdentifyingInfo(Component component) {

    // pulling inference into separate strategy
    return widgetIdentifier.identify(component);
  }
}
