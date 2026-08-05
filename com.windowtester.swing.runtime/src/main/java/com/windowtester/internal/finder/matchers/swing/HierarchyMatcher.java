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
package com.windowtester.internal.finder.matchers.swing;

import abbot.finder.Matcher;
import abbot.finder.matchers.ClassMatcher;
import com.windowtester.internal.swing.WidgetLocatorService;
import java.awt.Component;
import javax.swing.JPopupMenu;

/**
 * A matcher that matches widgets first against a target matcher (which might match a given class
 * and possibly name or label) and then by checking that parent criteria are met.
 * <p>
 * HierarchyMatchers are handy in making matches based on a widget's location in the widget
 * hierarchy.
 * <p>
 * For instance, to match a Text widget contained in the Group labeled "guests" in a Shell called
 * "Party Planner", we might write a HierarchyMatcher like this:<p>
 *
 * <pre>
 * new HierarchyMatcher(Text.class,
 * 			new HierarchyMatcher(Group.class, "guests",
 * 				new NameMatcher("Party Planner", Shell.class)));
 *
 * </pre>
 * <p>
 * Where containment is not enough, we can augment with indexes.  For example, suppose we want the
 * second Text widget (indexes are zero-indexed):
 *
 * <pre>
 *    - group: -------------------
 *   |     Text   Text     Text   |
 *    ----------------------------
 *
 *   new HierarchyMatcher(Text.class, 1,
 *           new HierarchyMatcher(Group.class, "group"));
 * </pre>
 *
 * <b>Note:</b> using the 0-index for an "only child" will not have the desired effect.  In fact,
 * it will fail. Only-child matches should NOT specify an index value (though they could use -1).
 * <p>
 * In other words, <code>new HierarchyMatcher(Text.class, 0, new
 * HierarchyMatcher(Group.class))</code>, will match the first Text child of a Group but only in the
 * event that that Text has siblings.  To match a Text only-child of a group, use a matcher
 * constructs like this <code>new HierarchyMatcher(Text.class, Group.class)</code> (or possibly like
 * this <code>new HierarchyMatcher(Text.class, -1, Group.class)</code>).
 */
public final class HierarchyMatcher implements Matcher {

  /**
   * A matcher composed of target class and name info
   */
  private final Matcher matcher;

  /**
   * A matcher to check parent criteria.
   */
  private final Matcher parentMatcher;

  /**
   * The index that identifies the target with respect to its siblings in the parent's list of
   * children.
   */
  private final int index;

  /**
   * The default index value for an unspecified index
   */
  private static final int DEFAULT_INDEX = -1;

  /**
   * Stateless helper for index/child queries. Shared across all match calls to avoid allocating a
   * new instance for every visited component.
   */
  private final WidgetLocatorService infoService = new WidgetLocatorService();

  /**
   * Create an instance.
   *
   * @param targetMatcher - a matcher to check target criteria.
   * @param parentMatcher - a matcher to check parent criteria.
   */
  public HierarchyMatcher(Matcher targetMatcher, Matcher parentMatcher) {
    this(DEFAULT_INDEX, targetMatcher, parentMatcher);
  }

  /**
   * Create an instance.
   *
   * @param index         - index that locates child with respect to its parent (in the parent's
   *                      list of children).
   * @param targetMatcher - a matcher to check target criteria.
   * @param parentMatcher - a matcher to check parent criteria.
   */
  public HierarchyMatcher(int index, Matcher targetMatcher, Matcher parentMatcher) {
    this.index = index;
    this.matcher = targetMatcher;
    this.parentMatcher = parentMatcher;
  }

  // convenience constructors

  /**
   * Create an instance.
   *
   * @param cls           - the class of the widget in question.
   * @param nameOrLabel   - the widget's name or label.
   * @param parentMatcher - a matcher to check parent criteria.
   */
  public HierarchyMatcher(Class<?> cls, String nameOrLabel, Matcher parentMatcher) {
    this(cls, nameOrLabel, DEFAULT_INDEX, parentMatcher);
  }

  /**
   * Create an instance.
   *
   * @param cls           - the class of the widget in question.
   * @param nameOrLabel   - the widget's name or label.
   * @param index         - the index of the widget in question.
   * @param parentMatcher - a matcher to check parent criteria.
   */
  public HierarchyMatcher(Class<?> cls, String nameOrLabel, int index, Matcher parentMatcher) {
    this(
        index,
        new CompositeMatcher(
            new Matcher[] {new ClassMatcher(cls), new NameOrLabelMatcher(nameOrLabel)}),
        parentMatcher);
  }

  /**
   * Create an instance.
   *
   * @param cls           - the class of the widget in question.
   * @param parentMatcher - a matcher to check parent criteria.
   */
  public HierarchyMatcher(Class<?> cls, Matcher parentMatcher) {
    this(cls, DEFAULT_INDEX, parentMatcher);
  }

  /**
   * Create an instance.
   *
   * @param cls           - the class of the widget in question.
   * @param index         - the index of the widget in question.
   * @param parentMatcher - a matcher to check parent criteria.
   */
  public HierarchyMatcher(Class<?> cls, int index, Matcher parentMatcher) {
    this(index, new ClassMatcher(cls), parentMatcher);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Matching
  //
  ///
  // ////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public boolean matches(Component component) {
    // is this log-worthy?
    if (component == null) {
      return false;
    }

    // the target criteria must hold in every case; evaluate it once
    if (!matcher.matches(component)) {
      return false;
    }

    Component parent = component.getParent();
    // If parent is  a JPopupMenu, get the parent menu
    if (parent instanceof JPopupMenu popupMenu) {
      parent = popupMenu.getInvoker();
    }

    // no parent criteria to check: the target match alone decides
    if (parent == null || parentMatcher == null) {
      return true;
    }

    // parent criteria present: parent must match and the child must sit at the expected index
    return parentMatcher.matches(parent) && infoService.getIndex(component, parent) == index;
  }

  @Override
  public void reset() {
    matcher.reset();
    if (parentMatcher != null) {
      parentMatcher.reset();
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Accessors
  //
  ///////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the matcher identifying the target of this match.
   */
  public Matcher getTargetMatcher() {
    return matcher;
  }

  /**
   * Get the matcher identifying the parent of the target of this match.
   */
  public Matcher getParentMatcher() {
    return parentMatcher;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Debugging
  //
  ///////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Hierarchy matcher (" + matcher + ", " + parentMatcher + ")";
  }
}
