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

import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.WidgetReference;
import java.awt.Component;
import java.awt.TextComponent;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.swing.AbstractButton;
import javax.swing.JLabel;

/**
 * Thrown when multiple widgets are found.
 */
public class MultipleWidgetsFoundException extends WidgetSearchException {

  public MultipleWidgetsFoundException(IWidgetLocator[] locators) {
    super(String.format("Multiple widgets found: %s", createLocatorClassDetails(locators)));
  }

  private static String createLocatorClassDetails(IWidgetLocator[] locators) {
    return Arrays.stream(locators)
        .filter(WidgetReference.class::isInstance)
        .map(WidgetReference.class::cast)
        .map(MultipleWidgetsFoundException::createDetailsText)
        .collect(Collectors.joining(","));
  }

  private static String createDetailsText(WidgetReference<?> ref) {
    var canonicalName = ref.getWidget().getClass().getCanonicalName();
    var name = getName(ref);
    var hashCode = ref.getWidget().hashCode();

    return canonicalName + "(" + name + "-" + hashCode + ")";
  }

  private static String getName(WidgetReference<?> ref) {
    var component = (Component) ref.getWidget();
    if (component instanceof JLabel label) {
      return label.getText();
    }
    if (component instanceof AbstractButton button) {
      return button.getText();
    }
    if (component instanceof TextComponent textComponent) {
      return textComponent.getText();
    }
    return component.getName();
  }
}
