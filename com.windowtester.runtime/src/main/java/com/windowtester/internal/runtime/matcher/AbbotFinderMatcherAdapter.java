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

import abbot.finder.Matcher;
import com.windowtester.runtime.locator.IWidgetMatcher;
import java.awt.Component;

/**
 * An adapter from an {@link IWidgetMatcher} to an Abbot {@link Matcher}.
 * <p>
 * Created using the {@link AdapterFactory#adapt(IWidgetMatcher)} creation method.
 */
/*package */ class AbbotFinderMatcherAdapter implements Matcher {

  private final IWidgetMatcher matcher;

  /**
   * Create an instance.
   *
   * @param widgetMatcher the matcher to adapt.
   */
  AbbotFinderMatcherAdapter(IWidgetMatcher<?> widgetMatcher) {
    matcher = widgetMatcher;
  }

  @Override
  public boolean matches(Component component) {
    return matcher.matches(component);
  }

  @Override
  public void reset() {
    matcher.reset();
  }
}
