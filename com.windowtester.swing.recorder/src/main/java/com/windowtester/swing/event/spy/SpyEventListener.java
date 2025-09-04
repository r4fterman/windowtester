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
package com.windowtester.swing.event.spy;

import com.windowtester.recorder.event.ISemanticEventListener;
import com.windowtester.recorder.event.IUISemanticEvent;
import com.windowtester.recorder.event.meta.RecorderErrorEvent;
import com.windowtester.recorder.event.meta.RecorderTraceEvent;

public class SpyEventListener implements ISemanticEventListener {

  @Override
  public void notify(IUISemanticEvent event) {}

  @Override
  public void notifyAssertionHookAdded(String hookName) {}

  @Override
  public void notifyControllerStart(int port) {}

  @Override
  public void notifyDisplayNotFound() {}

  @Override
  public void notifyDispose() {}

  @Override
  public void notifyError(RecorderErrorEvent event) {}

  @Override
  public void notifyPause() {}

  @Override
  public void notifyRestart() {}

  @Override
  public void notifySpyModeToggle() {
    SpyEventHandler.spyModeToggled();
  }

  @Override
  public void notifyStart() {}

  @Override
  public void notifyStop() {}

  @Override
  public void notifyTrace(RecorderTraceEvent event) {}

  @Override
  public void notifyWrite() {}
}
