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
package com.windowtester.runtime.internal.application;

/**
 * Application contexts describe interesting states that an application might be in (e.g., native vs. non-native).
 */
public interface IApplicationContext {

  IApplicationContext setNative();

  IApplicationContext setDefault();

  boolean isNative();

  boolean isDefault();
}
