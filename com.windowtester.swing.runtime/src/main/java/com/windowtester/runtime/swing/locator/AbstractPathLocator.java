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
import com.windowtester.internal.swing.matcher.ClassMatcher;
import com.windowtester.internal.swing.matcher.HierarchyMatcher;
import com.windowtester.internal.swing.matcher.IndexMatcher;
import com.windowtester.internal.swing.matcher.NameMatcher;
import com.windowtester.internal.swing.matcher.NameOrTextMatcher;
import com.windowtester.internal.swing.util.PathStringTokenizerUtil;
import com.windowtester.runtime.IClickDescription;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IPathLocator;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetMatcher;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.locator.WidgetReference;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import java.awt.Component;
import java.io.Serial;

/**
 * A base class for locators that have a path.
 */
public abstract class AbstractPathLocator extends SwingWidgetLocator implements IPathLocator {

  @Serial private static final long serialVersionUID = 4463096378279721331L;

  private String path;

  public AbstractPathLocator(Class<?> cls, String path) {
    this(cls, getLabel(path), path);
  }

  public AbstractPathLocator(Class<?> cls, String itemText, String path) {
    this(cls, itemText, path, UNASSIGNED, null);
  }

  public AbstractPathLocator(Class<?> cls, String path, SwingWidgetLocator parentInfo) {
    this(cls, getLabel(path), path, parentInfo);
  }

  public AbstractPathLocator(
      Class<?> cls, String itemText, String path, SwingWidgetLocator parentInfo) {
    this(cls, itemText, path, UNASSIGNED, parentInfo);
  }

  public AbstractPathLocator(Class<?> cls, String path, int index, SwingWidgetLocator parentInfo) {
    this(cls, getLabel(path), path, index, parentInfo);
  }

  public AbstractPathLocator(
      Class<?> cls, String itemText, String path, int index, SwingWidgetLocator parentInfo) {
    super(cls, itemText, index, parentInfo);
    this.path = path;
    this.matcher = createMatcher(cls, index, parentInfo);
  }

  private IWidgetMatcher<?> createMatcher(Class<?> cls, int index, SwingWidgetLocator parent) {
    IWidgetMatcher<?> matcher = null;
    if (this instanceof JListLocator || this instanceof JTreeItemLocator) {
      matcher = ClassMatcher.create(cls);
    } else if (cls != null) {
      matcher = new ExactClassMatcher(cls);
    }

    if (this instanceof JMenuItemLocator) {
      matcher = new CompoundMatcher(matcher, NameOrTextMatcher.create(getItemText()));
    }

    if (index != UNASSIGNED) {
      matcher = IndexMatcher.create(matcher, index);
    }

    if (parent != null) {
      // if parent is NamedWidgetLocator, then component has a name
      if (parent instanceof NamedWidgetLocator) {
        if (!(this instanceof JMenuItemLocator)) {
          matcher = new CompoundMatcher(matcher, NameMatcher.create(parent.getNameOrLabel()));
        }
      } else {
        if (index != UNASSIGNED) {
          matcher = HierarchyMatcher.create(matcher, parent.getMatcher(), index);
        } else {
          matcher = HierarchyMatcher.create(matcher, parent.getMatcher());
        }
      }
    }

    if (matcher != null) {
      return matcher;
    }

    var msg =
        String.format(
            "Unable to create matcher for class=%s, index=%d, parent=%s", cls, index, parent);
    throw new IllegalArgumentException(msg);
  }

  public String getPath() {
    return path;
  }

  public String getItemText() {
    if (this instanceof JListLocator || this instanceof JComboBoxLocator) {
      return path;
    }
    return getNameOrLabel();
  }

  private static String getLabel(String path) {
    if (path == null) {
      return null;
    }
    var items = PathStringTokenizerUtil.tokenize(path);
    return items[items.length - 1];
  }

  // not ideal to have this mutable but it fits best with current id inference scheme
  public void setPath(String pathString) {
    path = pathString;
  }

  /**
   * Perform the context click.
   *
   * @param ui           the UI context
   * @param widget       the widget reference to click
   * @param menuItemPath the path to the menu item to select
   * @return the clicked widget (as a reference)
   * @throws WidgetSearchException in case of error
   */
  @Override
  public IWidgetLocator contextClick(
      IUIContext ui, IWidgetReference widget, IClickDescription click, String menuItemPath)
      throws WidgetSearchException {
    var component = (Component) widget.getWidget();
    var clicked = ((UIContextSwing) ui).getDriver().contextClick(component, menuItemPath);
    return WidgetReference.create(clicked, this);
  }
}
