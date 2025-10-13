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

import abbot.Log;
import abbot.script.Action;
import abbot.script.ComponentReference;
import abbot.script.Resolver;
import abbot.script.Step;
import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowEvent;

/**
 * Record basic semantic events you might find on an Window. <p>
 * <p>
 * abbot.editor.recorder.WindowRecorder
 */
public class FrameRecorder extends WindowRecorder {

  private Frame frame;
  private int newState;

  public FrameRecorder(Resolver resolver) {
    super(resolver);
  }

  private final int WINDOW_STATE_CHANGED = 9 + WindowEvent.WINDOW_FIRST;

  @Override
  protected synchronized void init(int recordingType) {
    super.init(recordingType);
    frame = null;
  }

  @Override
  protected boolean isWindowEvent(AWTEvent event) {
    return ((event.getSource() instanceof Frame) && event.getID() == WINDOW_STATE_CHANGED)
        || super.isWindowEvent(event);
  }

  @Override
  protected boolean parseWindowEvent(AWTEvent event) {
    int id = event.getID();
    boolean consumed = true;
    if (id == WINDOW_STATE_CHANGED) {
      frame = (Frame) event.getSource();
      newState = getExtendedState(frame);
      setFinished(true);
    } else {
      consumed = super.parseWindowEvent(event);
    }
    return consumed;
  }

  @Override
  protected Step createStep() {
    if (getRecordingType() == SE_WINDOW && frame != null) {
      return createFrameStateChange(frame, newState);
    }
    return super.createStep();
  }

  protected Step createFrameStateChange(Frame frame, int newState) {
    // window tester - do we support this?
    ComponentReference ref = getResolver().addComponent(frame);
    return new Action(
        getResolver(),
        null,
        newState == Frame.NORMAL ? "actionNormalize" : "actionMaximize",
        new String[] {ref.getID()},
        Frame.class);
  }

  @Override
  protected Step createResize(Window window, Dimension size) {
    Step step = null;
    if (((Frame) window).isResizable()) {
      ComponentReference ref = getResolver().addComponent(window);
      step =
          new Action(
              getResolver(),
              null,
              "actionResize",
              new String[] {
                ref.getID(), String.valueOf(size.width), String.valueOf(size.height),
              },
              Frame.class);
    }
    return step;
  }

  protected int getExtendedState(Frame frame) {
    try {
      Integer state = (Integer) Frame.class.getMethod("getExtendedState").invoke(frame);
      Log.debug("State is " + state);
      return state;
    } catch (Exception e) {
      return frame.getState();
    }
  }
}
