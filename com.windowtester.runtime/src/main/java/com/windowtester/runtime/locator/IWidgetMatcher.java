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
package com.windowtester.runtime.locator;

/**
 * Widget Matchers are used to test whether a Widget matches some desired criteria.
 */
public interface IWidgetMatcher<T> {

  /**
   * Check whether the given Widget satisfies the specified criteria.
   *
   * @param widget the widget to test
   * @return <code>true</code> if the widget matches,
   * <code>false</code> otherwise
   */
  boolean matches(T widget);

  /**
   * Reset any traversal-scoped state before a fresh search.
   * <p>
   * Most matchers are stateless and use the default no-op. Order-dependent matchers (e.g. index
   * matchers that count occurrences during a single tree traversal) must reset their counters here
   * and propagate the reset to any delegate matchers, so that repeated searches with the same
   * matcher instance (for example while polling a wait condition) start from a clean state.
   */
  default void reset() {
    // no-op by default; stateful matchers override
  }
}
