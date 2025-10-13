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
package com.windowtester.internal.runtime.locator;

import com.windowtester.internal.runtime.Adapter;
import com.windowtester.internal.runtime.IWidgetIdentifier;
import com.windowtester.runtime.IAdaptable;
import com.windowtester.runtime.locator.ILocator;
import java.io.Serial;
import java.io.Serializable;

/**
 * Adapts a locator to an identifier (for playing nice with the legacy recorder).
 */
public class IdentifierAdapter implements IWidgetIdentifier, IAdaptable, ILocator, Serializable {

  @Serial private static final long serialVersionUID = -2449531209586204515L;

  private final transient ILocator locator;

  public IdentifierAdapter(ILocator locator) {
    this.locator = locator;
  }

  @Override
  public String getNameOrLabel() {
    return getLocator().toString();
  }

  @Override
  public Class<?> getTargetClass() {
    return null;
  }

  @Override
  public String getTargetClassName() {
    return null;
  }

  public ILocator getLocator() {
    return locator;
  }

  @Override
  public Object getAdapter(Class<?> adapter) {
    if (adapter == ILocator.class) {
      return getLocator();
    }
    return Adapter.adapt(getLocator(), adapter);
  }

  @Override
  public String toString() {
    return "IdentifierAdapter[" + getLocator() + "]";
  }
}
