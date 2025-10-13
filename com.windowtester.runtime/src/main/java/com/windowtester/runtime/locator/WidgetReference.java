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

import com.windowtester.internal.runtime.locator.IUISelector;
import com.windowtester.internal.runtime.locator.IUISelector2;
import com.windowtester.internal.runtime.system.WidgetSystem;
import com.windowtester.runtime.IAdaptable;
import com.windowtester.runtime.IUIContext;
import java.util.HashMap;
import java.util.Map;

/**
 * A widget locator that directly references a widget.  (Used to adapt widgets to widget locators.)
 */
public class WidgetReference<T> implements IWidgetReference, IAdaptable {

  /**
   * The referenced widget
   */
  private final T widget;

  /**
   * Adapters for the receiver indexed by class or <code>null</code> if not initialized by
   * {@link #addAdapter(Class, Object)}.
   */
  private Map<Class<?>, Object> adapters;

  /**
   * Create a widget reference locator for this widget.
   *
   * @param widget the widget
   * @return an <code>IWidgetLocator</code> instance that wraps this widget
   */
  public static <T> WidgetReference<T> create(T widget) {
    return new WidgetReference<>(widget);
  }

  /**
   * Create a widget reference locator for this widget.
   *
   * @param widget   the widget
   * @param selector selector for use in selecting the widget
   * @return an <code>IWidgetLocator</code> instance that wraps this widget
   */
  public static <T> WidgetReference<T> create(T widget, IUISelector selector) {
    var result = create(widget);
    result.addAdapter(IUISelector.class, selector);
    if (selector instanceof IUISelector2) {
      result.addAdapter(IUISelector2.class, selector);
    }
    return result;
  }

  public WidgetReference(T widget) {
    this.widget = widget;
  }

  @Override
  public IWidgetLocator[] findAll(IUIContext ui) {
    return new IWidgetLocator[] {this};
  }

  @Override
  public boolean matches(Object widget) {
    return this.widget == widget;
  }

  @Override
  public T getWidget() {
    return widget;
  }

  @Override
  public Object getAdapter(Class<?> adapter) {
    if (adapters != null) {
      Object result = adapters.get(adapter);
      if (result != null) {
        return result;
      }
    }
    if (IUISelector.class == adapter) {
      return WidgetSystem.getDefaultSelector(this);
    }

    if (adapters != null) {
      // next ask the IUISelector (if there is one)
      var selector = adapters.get(IUISelector.class);
      if (selector instanceof IAdaptable adaptable) {
        return adaptable.getAdapter(adapter);
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return "WidgetReference(" + widget + ")";
  }

  /**
   * Add an adapter to be returned by {@link #getAdapter(Class)}
   *
   * @param adapter the class (key)
   * @param value   the adapter object to be returned
   */
  public void addAdapter(Class<?> adapter, Object value) {
    if (adapters == null) {
      adapters = new HashMap<>();
    }
    adapters.put(adapter, value);
  }
}
