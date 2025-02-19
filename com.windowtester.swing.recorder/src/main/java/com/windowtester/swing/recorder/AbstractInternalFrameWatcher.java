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
package com.windowtester.swing.recorder;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public abstract class AbstractInternalFrameWatcher extends InternalFrameAdapter
    implements ComponentListener {
  public AbstractInternalFrameWatcher(JInternalFrame f) {
    f.addInternalFrameListener(this);
    f.addComponentListener(this);
  }

  protected abstract void dispatch(AWTEvent e);

  public void componentHidden(ComponentEvent e) {
    // Ensure this listener doesn't hang around after the frame goes
    // away.
    e.getComponent().removeComponentListener(this);
    ((JInternalFrame) e.getComponent()).removeInternalFrameListener(this);
  }

  public void internalFrameClosing(InternalFrameEvent e) {
    dispatch(e);
    e.getInternalFrame().removeInternalFrameListener(this);
    e.getInternalFrame().removeComponentListener(this);
  }

  public void internalFrameIconified(InternalFrameEvent e) {
    dispatch(e);
  }

  public void internalFrameDeiconified(InternalFrameEvent e) {
    dispatch(e);
  }

  public void componentShown(ComponentEvent e) {}

  public void componentResized(ComponentEvent e) {}

  public void componentMoved(ComponentEvent e) {}
}
