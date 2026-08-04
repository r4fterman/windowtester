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
package com.windowtester.internal.runtime.matcher;

import com.windowtester.runtime.locator.IWidgetMatcher;

/**
 * Compounds/composes/aggregates Matchers.
 */
public class CompoundMatcher implements IWidgetMatcher {

  private final IWidgetMatcher componentMatcher1;
  private final IWidgetMatcher componentMatcher2;

  public static IWidgetMatcher create(IWidgetMatcher m1, IWidgetMatcher m2) {
    return new CompoundMatcher(m1, m2);
  }

  public CompoundMatcher(IWidgetMatcher m1, IWidgetMatcher m2) {
    componentMatcher1 = m1;
    componentMatcher2 = m2;
  }

  @Override
  public boolean matches(Object widget) {
    // short-circuit: skip the second (often more expensive text) match when the first fails
    return componentMatcher1.matches(widget) && componentMatcher2.matches(widget);
  }

  @Override
  public void reset() {
    componentMatcher1.reset();
    componentMatcher2.reset();
  }
}
