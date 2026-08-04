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
package util;

import abbot.util.EventNormalizer;
import abbot.util.SingleThreadedEventListener;
import com.windowtester.recorder.event.ISemanticEvent;
import com.windowtester.recorder.event.user.SemanticEventAdapter;
import com.windowtester.swing.event.recorder.ConsoleReportingListener;
import com.windowtester.swing.event.recorder.EventCachingListener;
import com.windowtester.swing.event.recorder.SwingGuiTestRecorder;
import com.windowtester.swing.recorder.RecordingFailedException;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.MenuComponent;

public class SwingEventRecordingWatcher {

  private static final long FIXTURE_EVENT_MASK =
      abbot.editor.recorder.EventRecorder.RECORDING_EVENT_MASK;

  private static SwingGuiTestRecorder recorder;

  /**
   * A cache to store recorded events
   */
  private final EventCachingListener cache = new EventCachingListener();

  private static final EventNormalizer normalizer = new EventNormalizer();

  public SwingEventRecordingWatcher() {
    recorder = new SwingGuiTestRecorder();
    recorder.addListener(new ConsoleReportingListener());
    recorder.addListener(cache);
    recorder.addListener(
        new SemanticEventAdapter() {
          public void notifyDispose() {
            codegen();
          }
        });
  }

  /**
   * Watch for events;
   */
  public void watch() {
    //	record();
    recorder.start();
    waitForDisposeLoop();
  }

  public void codegen() {
    // do nothing
  }

  public ISemanticEvent[] getEvents() {
    return cache.getEvents();
  }

  private static void waitForDisposeLoop() {}

  private static void record() {
    normalizer.startListening(
        new SingleThreadedEventListener() {
          protected void processEvent(final AWTEvent event) {
            startRecordingEvent(event);
          }
        },
        FIXTURE_EVENT_MASK);
  }

  /**
   * The  events are sent to the recorder.
   */
  public static void startRecordingEvent(AWTEvent event) {
    Object src = event.getSource();
    boolean isComponent = src instanceof Component;

    // Allow only component events and AWT menu actions
    if (!isComponent && !(src instanceof MenuComponent)) {
      return;
    }

    if (recorder != null) {
      try {
        recorder.startRecordingEvent(event);
      } catch (RecordingFailedException e) {
        // Stop recording but keep what we've got so far
        recorder.stop();
        e.printStackTrace();
      }
    }
  }

  /**
   * Stop recording and update the recorder actions' state.
   */
  public static void stopRecording(boolean discardRecording) {
    recorder.terminate();
  }
}
