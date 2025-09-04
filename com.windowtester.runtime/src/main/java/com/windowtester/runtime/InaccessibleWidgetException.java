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
package com.windowtester.runtime;

import java.io.Serial;

/**
 * Thrown when a matching widget is found, but it is not of the necessary type or does not have the
 * appropriate accessor method. For example, the {@link SwingWidgetLocator} throws this if
 * getText(IUIContext) is called and the widget found does not have a getText() method.
 */
public class InaccessibleWidgetException extends WidgetSearchException {

  @Serial private static final long serialVersionUID = 3035842156651482281L;

  public InaccessibleWidgetException() {
    super();
  }

  public InaccessibleWidgetException(String msg) {
    super(msg);
  }

  public InaccessibleWidgetException(Throwable cause) {
    super(cause);
  }
}
