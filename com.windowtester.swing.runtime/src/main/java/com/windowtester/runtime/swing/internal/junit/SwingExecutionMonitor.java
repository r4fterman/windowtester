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
package com.windowtester.runtime.swing.internal.junit;

import abbot.finder.AWTHierarchy;
import abbot.finder.Hierarchy;
import com.windowtester.internal.runtime.junit.core.AbstractExecutionMonitor;
import com.windowtester.internal.runtime.junit.core.ITestIdentifier;
import com.windowtester.internal.swing.UIContextSwingFactory;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.monitor.IUIThreadMonitor;
import java.awt.Component;
import java.awt.Window;

/**
 * Monitor for executing Swing tests.
 */
public class SwingExecutionMonitor extends AbstractExecutionMonitor {

  private IUIContext uiContext;

  public SwingExecutionMonitor() {
    // do nothing
  }

  @Override
  public void testStarting(ITestIdentifier identifier) {
    // TODO [author=Dan] Race condition workaround... better solution needed...
    // If a Swing test launches an application, but a Swing window is still
    // open from a previous test, then the "notShowing" loop below
    // will detect the window from the first application and exit immediately
    // without waiting for the 2nd application to open its window.
    // This hack sleeps for a moment, giving the Swing UI thread a chance
    // to open the window. A better solution would be to get a message
    // before the 2nd application is launched to cache the active window
    // and wait until the active window has changed.
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // ignore
    }

    /*
     * wait for frame showing and active
     * before starting the test
     */
    boolean notShowing = true;
    while (notShowing) {
      Hierarchy hierarchy = AWTHierarchy.getDefault();
      if (hierarchy.getRoots().isEmpty()) {
        notShowing = false;
      } else {
        for (Component c : hierarchy.getRoots()) {
          if (((Window) c).isActive()) {
            notShowing = false;
            break;
          }
        }
      }
    }

    // next inform listeners, etc.
    super.testStarting(identifier);
  }

  @Override
  protected void doWaitForFinish() {}

  @Override
  protected boolean terminateWaitForFinish() {
    var hierarchy = AWTHierarchy.getDefault();
    return hierarchy.getRoots().isEmpty();
  }

  @Override
  protected IUIThreadMonitor getUIThreadMonitor() {
    return (IUIThreadMonitor) getUI().getAdapter(IUIThreadMonitor.class);
  }

  /**
   * Get the UI Context.
   */
  public IUIContext getUI() {
    if (uiContext == null) {
      uiContext = UIContextSwingFactory.createContext();
    }
    return uiContext;
  }
}
