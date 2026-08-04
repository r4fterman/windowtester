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

import abbot.Log;
import abbot.finder.Matcher;
import java.awt.Component;

public class IndexMatcher implements Matcher {

  private final Matcher matcher;
  private final int index;
  private int current = -1;

  public IndexMatcher(Matcher matcher, int index) {
    this.index = index;
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Component component) {
    boolean matches = false;
    if (matcher.matches(component)) {
      current++;
      Log.debug(
          "Found match for matcher:\n"
              + matcher
              + "\n Must check index:["
              + current
              + "=="
              + index
              + "]");
      if (current == index) {
        matches = true;
      }
    }
    return matches;
  }

  @Override
  public void reset() {
    current = -1;
    matcher.reset();
  }

  public String toString() {
    return "Index Matcher (" + matcher + ", " + index + ")";
  }
}
