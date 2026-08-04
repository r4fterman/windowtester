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
package com.windowtester.swing.event.recorder;

import abbot.editor.recorder.RecordingFailedException;
import abbot.finder.AWTHierarchy;
import abbot.finder.Hierarchy;
import abbot.script.Script;
import abbot.util.EventNormalizer;
import abbot.util.SingleThreadedEventListener;
import com.windowtester.recorder.IEventFilter;
import com.windowtester.recorder.IEventRecorder;
import com.windowtester.recorder.event.ISemanticEventListener;
import com.windowtester.recorder.event.IUISemanticEvent;
import com.windowtester.recorder.event.meta.RecorderErrorEvent;
import com.windowtester.recorder.event.meta.RecorderTraceEvent;
import com.windowtester.swing.event.spy.SpyEventListener;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.MenuComponent;

/**
 * The Swing Gui Test Recorder
 */
public class SwingGuiTestRecorder implements IEventRecorder {

  private static final long FIXTURE_EVENT_MASK =
      abbot.editor.recorder.EventRecorder.RECORDING_EVENT_MASK;

  private final SwingEventRecorder recorder;

  // the listener
  private static final EventNormalizer normalizer = new EventNormalizer();

  /**
   * Create an instance
   */
  public SwingGuiTestRecorder() {
    Hierarchy hierarchy = AWTHierarchy.getDefault();
    // for compatibility with abbot code, nothing to do with windowtester
    Script script = new Script(hierarchy);
    recorder = new SwingEventRecorder(script, true);
    EventCachingListener cache = new EventCachingListener();
    recorder.addListener(cache);
    recorder.addListener(new SpyEventListener());
  }

  @Override
  public void start() {
    startListening();
    recorder.start();
  }

  @Override
  public void stop() {
    recorder.stop();
  }

  @Override
  public void write() {
    recorder.write();
  }

  @Override
  public void restart() {
    recorder.restart();
  }

  @Override
  public void terminate() {
    recorder.terminate();
  }

  @Override
  public void toggleSpyMode() {
    recorder.toggleSpyMode();
  }

  @Override
  public void pause() {
    recorder.pause();
  }

  @Override
  public void addListener(ISemanticEventListener listener) {
    recorder.addListener(listener);
  }

  @Override
  public void removeListener(ISemanticEventListener listener) {
    recorder.removeListener(listener);
  }

  @Override
  public void record(IUISemanticEvent semanticEvent) {
    recorder.record(semanticEvent);
  }

  @Override
  public void reportError(RecorderErrorEvent event) {
    recorder.reportError(event);
  }

  @Override
  public void trace(RecorderTraceEvent event) {
    recorder.trace(event);
  }

  @Override
  public void addEventFilter(IEventFilter filter) {
    recorder.addEventFilter(filter);
  }

  @Override
  public void removeEventFilter(IEventFilter filter) {
    recorder.removeEventFilter(filter);
  }

  @Override
  public void addHook(String hookName) {
    recorder.addHook(hookName);
  }

  /**
   * start listening for events
   */
  private void startListening() {
    normalizer.startListening(
        new SingleThreadedEventListener() {
          @Override
          protected void processEvent(AWTEvent event) {
            startRecordingEvent(event);
          }
        },
        FIXTURE_EVENT_MASK);
  }

  /**
   * The events are sent to the recorder.
   */
  public void startRecordingEvent(AWTEvent event) {
    Object src = event.getSource();
    boolean isComponent = src instanceof Component;

    // Allow only component events and AWT menu actions
    if (!isComponent && !(src instanceof MenuComponent)) {
      System.out.println("Source not a Component or MenuComponent: " + event);
      return;
    }

    if (recorder != null) {
      try {
        recorder.record(event);
      } catch (RecordingFailedException e) {
        // Stop recording but keep what we've got so far
        recorder.stop();
      }
    }
  }

  /* (non-Javadoc)
   * @see com.windowtester.recorder.IEventRecorder#isRecording()
   */
  @Override
  public boolean isRecording() {
    return recorder.isRecording();
  }
}
