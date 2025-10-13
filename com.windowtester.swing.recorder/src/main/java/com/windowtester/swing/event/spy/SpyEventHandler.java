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

import com.windowtester.internal.runtime.IWidgetIdentifier;
import com.windowtester.internal.swing.UIContextSwingFactory;
import com.windowtester.internal.swing.WidgetLocatorService;
import com.windowtester.recorder.event.user.SemanticWidgetInspectionEvent;
import com.windowtester.recorder.event.user.UISemanticEvent;
import com.windowtester.recorder.event.user.UISemanticEvent.EventInfo;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;

public class SpyEventHandler {

  // FOR TESTING
  private static boolean FORCE_ENABLE = false;
  private static boolean inSpyMode = FORCE_ENABLE;

  public UISemanticEvent interpretHover(AWTEvent event) {
    if (!inSpyMode) {
      return null;
    }
    return createInspectionEvent(event);
  }

  public static void spyModeToggled() {
    inSpyMode = !inSpyMode;
    //		Tracer.trace(IEventRecorderPluginTraceOptions.SWT_EVENTS, "spy mode toggled to: " +
    // inSpyMode);
  }

  private UISemanticEvent createInspectionEvent(AWTEvent event) {
    MouseEvent me = (MouseEvent) event;

    Component component = me.getComponent();

    if (component == null) {
      return null;
    }

    component = getMostSpecificWidgetForEvent(component, event);

    EventInfo info = extractInfo(event, component);
    return new SemanticWidgetInspectionEvent(info, UIContextSwingFactory.createContext())
        .withWidgetHash(component.hashCode())
        .atHoverPoint(getCursorPosition());
  }

  private Point getCursorPosition() {
    return MouseInfo.getPointerInfo().getLocation();
  }

  private EventInfo extractInfo(AWTEvent event, Component w) {
    EventInfo info = new EventInfo();
    info.toString = "inspection request for: " + w;
    info.cls = w.getClass().getName();
    info.hierarchyInfo = identifyWidget(w, event);
    if (event instanceof MouseEvent) {
      MouseEvent me = (MouseEvent) event;
      info.x = me.getX();
      info.y = me.getY();
    }
    return info;
  }

  private IWidgetIdentifier identifyWidget(Component w, AWTEvent event) {

    IWidgetIdentifier id = new WidgetLocatorService().inferIdentifyingInfo(w);
    return sanityCheck(id, event);
  }

  private IWidgetIdentifier sanityCheck(IWidgetIdentifier id, AWTEvent event) {
    return id;
  }

  static Component getMostSpecificWidgetForEvent(Component w, AWTEvent event) {
    return w;
  }
}
