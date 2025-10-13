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
package com.windowtester.internal.runtime.selector;

import com.windowtester.internal.runtime.locator.IUISelector;
import com.windowtester.internal.runtime.system.WidgetSystem;
import com.windowtester.runtime.ClickDescription;
import com.windowtester.runtime.IAdaptable;
import com.windowtester.runtime.IClickDescription;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IItemLocator;
import com.windowtester.runtime.locator.ILocator;
import com.windowtester.runtime.locator.IMenuItemLocator;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.locator.XYLocator;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class that parses and performs click commands.
 */
public class ClickHelper implements IClickDriver {

  private final IUIContext ui;
  private List<Listener> listeners;

  public ClickHelper(IUIContext ui) {
    this.ui = ui;
  }

  @Override
  public IWidgetLocator click(int clickCount, ILocator locator, int buttonMask)
      throws WidgetSearchException {
    var widgetLocator = getWidgetLocator(locator);
    IWidgetReference widget = null;
    if (!(widgetLocator instanceof IItemLocator)) {
      widget = doFind(widgetLocator);
    }

    // create click description
    var click = createClickDescription(clickCount, locator, buttonMask);

    var selector = getSelector(widgetLocator);
    var clicked = doClick(widget, click, selector);

    informClick(click, clicked);

    return clicked;
  }

  @Override
  public IWidgetLocator contextClick(ILocator locator, IMenuItemLocator menuItem)
      throws WidgetSearchException {
    var wl = getWidgetLocator(locator);

    IWidgetReference widget = null;
    if (!(wl instanceof IItemLocator)) {
      widget = doFind(wl);
    }

    // create click description
    var click = createClickDescription(1, locator, WT.BUTTON3);

    var selector = getSelector(wl);
    var clicked = doContextClick(menuItem, widget, click, selector);

    informContextClick(click, clicked);

    return clicked;
  }

  /**
   * Get the selector associated with this locator.
   */
  private IUISelector getSelector(ILocator locator) {
    if (locator instanceof IUISelector l) {
      return l;
    }

    if (locator instanceof IAdaptable adaptable) {
      var selector = (IUISelector) adaptable.getAdapter(IUISelector.class);
      if (selector != null) {
        return selector;
      }
    }

    if (locator instanceof IWidgetReference reference) {
      return WidgetSystem.getDefaultSelector(reference.getWidget());
    }

    throw new IllegalStateException();
  }

  /**
   * Get the WidgetLocator associated with this ILocator.
   */
  public static IWidgetLocator getWidgetLocator(ILocator locator) {
    if (locator instanceof IWidgetLocator widgetLocator) {
      return widgetLocator;
    }

    if (locator instanceof XYLocator xyLocator) {
      return getWidgetLocator(xyLocator.locator());
    }

    // SHOULD THROW EXCEPTION HERE?
    return null;
  }

  private IClickDescription createClickDescription(
      int clickCount, ILocator locator, int buttonMask) {
    // TODO properly handle nested XYLocators
    return ClickDescription.create(clickCount, locator, buttonMask);
  }

  private IUIContext getUIContext() {
    return ui;
  }

  @Override
  public void addClickListener(Listener listener) {
    getListeners().add(listener);
  }

  private void informClick(IClickDescription click, IWidgetLocator widgetLocator) {
    getListeners().forEach(listener -> listener.clicked(click, widgetLocator));
  }

  private void informContextClick(IClickDescription click, IWidgetLocator widgetLocator) {
    getListeners().forEach(listener -> listener.contextClicked(click, widgetLocator));
  }

  private List<Listener> getListeners() {
    if (listeners == null) {
      listeners = new ArrayList<>();
    }
    return listeners;
  }

  private IWidgetLocator doClick(
      IWidgetReference widget, IClickDescription click, IUISelector selector)
      throws WidgetSearchException {
    return selector.click(getUIContext(), widget, click);
  }

  private IWidgetReference doFind(IWidgetLocator widgetLocator) throws WidgetSearchException {
    return (IWidgetReference) getUIContext().find(widgetLocator);
  }

  private IWidgetLocator doContextClick(
      IMenuItemLocator menuItem,
      IWidgetReference widget,
      IClickDescription click,
      IUISelector selector)
      throws WidgetSearchException {
    return selector.contextClick(getUIContext(), widget, click, menuItem.getPath());
  }
}
