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
package com.windowtester.internal.runtime;

/**
 * This interface marks classes that can be used to uniquely identify widgets in a Widget hierarchy.
 */
public interface IWidgetIdentifier {

  /**
   * Minimally locators tend to have a name or label.  (Note this may be <code>null</code>.)
   */
  String getNameOrLabel();

  /**
   * The target widget's class or <code>null</null> if unknown.
   */
  Class<?> getTargetClass();

  /**
   * The target widget's class name or <code>null</null> if unknown.
   */
  String getTargetClassName();
}
