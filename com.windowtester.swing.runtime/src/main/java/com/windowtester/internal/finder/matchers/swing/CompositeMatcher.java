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
import abbot.finder.matchers.AbstractMatcher;
import java.awt.Component;

/**
 * This matcher does not have anything to do with org.eclipse.swt.widgets.Composite; rather, it allows searches for
 * widgets using several matchers.  The specified matchers are ANDed together so that a given widget matches in the
 * CompositeMatcher if and only if the widget matches in all of the component matchers.  Nulls in the array of matchers
 * are ignored
 */
public class CompositeMatcher extends AbstractMatcher {
  private final Matcher[] matchers;

  public CompositeMatcher(Matcher[] matchers) {
    this.matchers = matchers;
  }

  @Override
  public boolean matches(final Component component) {
    boolean atLeastOneMatcherPresent = false; /* If that construction this should be checked! */
    for (Matcher matcher : matchers) {
      if (matcher != null) {
        atLeastOneMatcherPresent = true;
        if (!matcher.matches(component)) {
          // ANDed together: a single failure means no match, stop evaluating the rest
          return false;
        }
      }
    }
    return atLeastOneMatcherPresent;
  }

  @Override
  public void reset() {
    for (Matcher matcher : matchers) {
      if (matcher != null) {
        matcher.reset();
      }
    }
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder
        .append("Composite matcher with ")
        .append(matchers.length)
        .append(" component matchers:\n");
    for (int i = 0; i < matchers.length; i++) {
      builder.append("[").append(i).append("] ").append(matchers[i].toString()).append("\n");
    }
    return builder.toString();
  }
}
