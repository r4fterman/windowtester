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
 * An adapter from an Abbot {@link Matcher} to an {@link IWidgetMatcher}.
 * <p>
 * Created using the {@link AdapterFactory#adapt(Matcher)} creation method.
 */
public class WidgetMatcherAdapter implements IWidgetMatcher<Component> {

  private final Matcher matcher;

  /**
   * Create an instance.
   *
   * @param matcher the matcher to adapt
   */
  public WidgetMatcherAdapter(Matcher matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Component widget) {
    return matcher.matches(widget);
  }

  @Override
  public void reset() {
    matcher.reset();
  }

  @Override
  public String toString() {
    return matcher.toString();
  }
}
