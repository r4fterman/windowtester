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

import abbot.finder.AWTHierarchy;
import abbot.finder.Hierarchy;
import com.windowtester.junit5.WindowtesterExtension;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// import contactmanager.ContactManagerSwing;

/**
 * A simple recording jig for swing recording/codegen debugging.
 * <p>
 * To use, launch in a JUnit test
 * <p>
 * In the Arguments tab, for VM arguments, add the following -Djava.library.path=c:\eclipse-SDK-3.2\swt
 * <p>
 * the path to the directory for the swt.jar and the dll files .
 *
 * @author Phil Quitslund
 * @author keertip
 */
@ExtendWith(WindowtesterExtension.class)
class SwingEventRecordingJig {

  // private static final boolean DISPLAY_EVENTS = true;
  private static final Object lock = new Object();
  private SwingEventRecordingWatcher watcher;

  private WindowListener listener =
      new WindowAdapter() {
        public void windowClosing(WindowEvent w) {
          // remove lock
          SwingEventRecordingWatcher.stopRecording(false);
          synchronized (lock) {
            lock.notifyAll();
          }
        }
      };

  @Test
  public void testDrive() {
    // get the application frame and attach the listener
    Frame f, frame = null;
    Hierarchy h = AWTHierarchy.getDefault();
    Iterator<Component> i = h.getRoots().iterator();
    boolean done = false;
    while (i.hasNext() && !done) {
      f = (Frame) i.next();
      if (!Objects.equals(f.getTitle(), "Abbot Robot Verification")) {
        frame = f;
        done = true;
      }
    }
    if (frame != null) {
      frame.addWindowListener(listener);
    }
    // just watch!
    watcher = new SwingEventRecordingWatcher();
    watcher.watch();

    // wait for lock to be released
    try {
      synchronized (lock) {
        lock.wait();
      }
      Thread.sleep(50000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
