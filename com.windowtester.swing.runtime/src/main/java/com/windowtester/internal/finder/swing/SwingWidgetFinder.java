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
package com.windowtester.internal.finder.swing;

import abbot.finder.AWTHierarchy;
import abbot.finder.Hierarchy;
import abbot.finder.Matcher;
import com.windowtester.internal.runtime.finder.IWidgetFinder;
import com.windowtester.internal.runtime.matcher.AdapterFactory;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.WidgetReference;
import java.awt.Component;
import java.awt.Window;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** A Swing Widget Finder. */
public class SwingWidgetFinder implements IWidgetFinder {

  private static final IWidgetFinder DEFAULT = new SwingWidgetFinder(new AWTHierarchy());

  public static IWidgetFinder getDefault() {
    return DEFAULT;
  }

  private final Hierarchy hierarchy;

  public SwingWidgetFinder() {
    this(AWTHierarchy.getDefault());
  }

  public SwingWidgetFinder(Hierarchy h) {
    hierarchy = h;
  }

  @Override
  public IWidgetLocator[] findAll(IWidgetLocator locator) {
    var matcher = new AdapterFactory().adapt(locator);

    var matchingComponents = new HashSet<Component>();
    addMatchingComponents(matcher, matchingComponents);

    return matchingComponents.stream()
        .map(WidgetReference::new)
        .map(IWidgetLocator.class::cast)
        .toArray(IWidgetLocator[]::new);
  }

  private void addMatchingComponents(Matcher matcher, HashSet<Component> found) {
    Arrays.stream(Window.getWindows())
        .filter(Window::isDisplayable)
        .filter(this::isMatchingWindow)
        .forEach(window -> findMatches(matcher, window, found));
  }

  private boolean isMatchingWindow(Window window) {
    if (window.isActive()) {
      // match only if window has focus
      return true;
    }
    if (window.isVisible()) {
      return true;
    }

    if (window.getOwnedWindows().length > 0) {
      return true;
    }

    if (window.getClass().getName().equals("sun.awt.EmbeddedFrame")
        || (window.getClass().getName().equals("sun.awt.windows.WEmbeddedFrame"))) {
      // Embedded Frames are not accessible in Apple's Java5+
      // 12/3/09 : added WEmbeddedFrame
      return true;
    }

    // check to see whether (ALL) frame owns any windows
    return Arrays.stream(window.getOwnedWindows()).anyMatch(this::isMatchingWindow);
  }

  private void findMatches(Matcher matcher, Component component, Set<Component> found) {
    hierarchy.getComponents(component).forEach(comp -> findMatches(matcher, comp, found));

    if (matcher.matches(component)) {
      found.add(component);
    }
  }
}
