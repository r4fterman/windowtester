/*******************************************************************************
 *  Copyright (component) 2012 Google, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Google, Inc. - initial API and implementation
 *******************************************************************************/
package com.windowtester.internal.swing.locator;

import abbot.finder.AWTHierarchy;
import abbot.finder.ComponentFinder;
import abbot.finder.ComponentNotFoundException;
import abbot.finder.Hierarchy;
import abbot.finder.Matcher;
import abbot.finder.MultiMatcher;
import abbot.finder.MultipleComponentsFoundException;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 * Provides basic component lookup, examining each component in turn. Searches all components of
 * interest in a given hierarchy.
 */
public class BasicFinder2 implements ComponentFinder {

  private static final ComponentFinder DEFAULT = new BasicFinder2(new AWTHierarchy());

  public static ComponentFinder getDefault() {
    return DEFAULT;
  }

  private final Hierarchy hierarchy;

  private class SingleComponentHierarchy implements Hierarchy {

    private final Component root;
    private final List<Component> list = new ArrayList<>();

    public SingleComponentHierarchy(Container root) {
      this.root = root;
      list.add(root);
    }

    public Collection<Component> getRoots() {
      return list;
    }

    public Collection<Component> getComponents(Component component) {
      return getHierarchy().getComponents(component);
    }

    public Container getParent(Component component) {
      return getHierarchy().getParent(component);
    }

    public boolean contains(Component component) {
      return getHierarchy().contains(component) && SwingUtilities.isDescendingFrom(component, root);
    }

    public void dispose(Window window) {
      getHierarchy().dispose(window);
    }
  }

  public BasicFinder2() {
    this(AWTHierarchy.getDefault());
  }

  public BasicFinder2(Hierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  protected Hierarchy getHierarchy() {
    return hierarchy;
  }

  /**
   * Find a Component, using the given Matcher to determine whether a given component in the
   * hierarchy under the given root is the desired one.
   */
  @Override
  public Component find(Container root, Matcher matcher)
      throws ComponentNotFoundException, MultipleComponentsFoundException {
    var hierarchy = root != null ? new SingleComponentHierarchy(root) : getHierarchy();
    return find(hierarchy, matcher);
  }

  /**
   * Find a Component, using the given Matcher to determine whether a given component in the
   * hierarchy used by this ComponentFinder is the desired one.
   */
  @Override
  public Component find(Matcher matcher)
      throws ComponentNotFoundException, MultipleComponentsFoundException {
    return find(getHierarchy(), matcher);
  }

  protected Component find(Hierarchy hierarchy, Matcher matcher)
      throws ComponentNotFoundException, MultipleComponentsFoundException {
    var found = new HashSet<Component>();
    for (Component component : hierarchy.getRoots()) {
      findMatches(hierarchy, matcher, component, found);
    }

    if (found.isEmpty()) {
      throw new ComponentNotFoundException("finder.not_found");
    } else if (found.size() > 1) {
      var list = found.toArray(new Component[0]);
      if (!(matcher instanceof MultiMatcher)) {
        throw new MultipleComponentsFoundException("finder.multiple_found", list);
      }
      return ((MultiMatcher) matcher).bestMatch(list);
    }
    return found.iterator().next();
  }

  protected void findMatches(
      Hierarchy hierarchy, Matcher matcher, Component component, Set<Component> found) {
    if (found.size() == 1 && !(matcher instanceof MultiMatcher)) {
      return;
    }

    for (Component comp : hierarchy.getComponents(component)) {
      findMatches(hierarchy, matcher, comp, found);
    }
    if (matcher.matches(component)) {
      found.add(component);
    }
  }

  /**
   * Find a Component, using the given Matcher to determine whether a given component in the
   * hierarchy used by this ComponentFinder is the desired one.
   */
  public int findAll(Matcher matcher) {
    return findAll(getHierarchy(), matcher);
  }

  protected int findAll(Hierarchy hierarchy, Matcher matcher) {
    var found = new HashSet<Component>();
    for (Component component : hierarchy.getRoots()) {
      // 2/22/07 : kp check for match only in active window
      if (((Window) component).isActive()) {
        findMatchesAll(hierarchy, matcher, component, found);
      }
    }
    if (found.isEmpty()) {
      // component not found ??
      return 0;
    } else if (found.size() > 1) {
      // multiple components found for the locator
      return -1;
    }
    return 1;
  }

  protected void findMatchesAll(
      Hierarchy hierarchy, Matcher matcher, Component component, Set<Component> found) {
    for (Component comp : hierarchy.getComponents(component)) {
      findMatchesAll(hierarchy, matcher, comp, found);
    }

    if (matcher.matches(component)) {
      found.add(component);
    }
  }
}
