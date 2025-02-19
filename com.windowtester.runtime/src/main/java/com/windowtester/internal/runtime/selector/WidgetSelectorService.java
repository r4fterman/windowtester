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

import com.windowtester.runtime.IWidgetSelectorDelegate;
import java.util.HashMap;
import java.util.Map;

public class WidgetSelectorService implements IWidgetSelectorService {

  private static final WidgetSelectorService INSTANCE = new WidgetSelectorService();

  Map _map = new HashMap();

  private WidgetSelectorService() {}

  public void set(Class widgetClass, IWidgetSelectorDelegate selector) {
    _map.put(widgetClass, selector);
  }

  public IWidgetSelectorDelegate get(Class widgetClass) {
    return (IWidgetSelectorDelegate) _map.get(widgetClass);
  }

  public static IWidgetSelectorService getInstance() {
    return INSTANCE;
  }
}
