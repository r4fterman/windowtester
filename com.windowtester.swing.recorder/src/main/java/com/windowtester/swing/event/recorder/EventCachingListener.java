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

import com.windowtester.recorder.event.ISemanticEvent;
import com.windowtester.recorder.event.ISemanticEventListener;
import com.windowtester.recorder.event.IUISemanticEvent;
import com.windowtester.recorder.event.meta.RecorderAssertionHookAddedEvent;
import com.windowtester.recorder.event.meta.RecorderErrorEvent;
import com.windowtester.recorder.event.meta.RecorderTraceEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A listener that caches recorded events to the console.
 */
public class EventCachingListener implements ISemanticEventListener {

  private final List<ISemanticEvent> events = new ArrayList<>();

  /**
   * A describer used for stateful event descriptions
   */
  // private EventDescriber _describer = new EventDescriber();

  @Override
  public void notify(IUISemanticEvent event) {
    events.add(event);
  }

  @Override
  public void notifyAssertionHookAdded(String hookName) {
    events.add(new RecorderAssertionHookAddedEvent(hookName));
  }

  @Override
  public void notifyStart() {}

  @Override
  public void notifyStop() {}

  @Override
  public void notifyWrite() {}

  @Override
  public void notifyPause() {}

  @Override
  public void notifyDispose() {}

  @Override
  public void notifyRestart() {}

  @Override
  public void notifyError(RecorderErrorEvent event) {}

  @Override
  public void notifyTrace(RecorderTraceEvent event) {}

  @Override
  public void notifySpyModeToggle() {}

  public ISemanticEvent[] getEvents() {
    if (events.isEmpty()) {
      return new IUISemanticEvent[0];
    }
    return events.toArray(new ISemanticEvent[0]);
  }

  @Override
  public void notifyControllerStart(int port) {}

  @Override
  public void notifyDisplayNotFound() {}
}
