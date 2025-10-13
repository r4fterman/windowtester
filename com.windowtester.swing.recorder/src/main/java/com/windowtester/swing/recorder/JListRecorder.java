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

import abbot.script.Action;
import abbot.script.ComponentReference;
import abbot.script.Resolver;
import abbot.script.Step;
import com.windowtester.recorder.event.IUISemanticEvent;
import com.windowtester.recorder.event.UISemanticEventFactory;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;

/**
 * Record basic semantic events you might find on an JList. <p>
 * <ul>
 * <li>Select a cell
 * </ul>
 * Added windowTester semantic event generation
 */
public class JListRecorder extends JComponentRecorder {

  public JListRecorder(Resolver resolver) {
    super(resolver);
  }

  @Override
  protected Step createClick(Component target, int x, int y, int mods, int count) {
    // create windowtester semantic event
    IUISemanticEvent semanticEvent =
        UISemanticEventFactory.createListSelectionEvent(
            (JList) target, x, y, mods, count, getButton());
    notify(semanticEvent);

    // abbot code
    JList<?> list = (JList<?>) target;
    ComponentReference cr = getResolver().addComponent(target);
    String methodName = "actionSelectRow";

    List<String> args = new ArrayList<>();
    args.add(cr.getID());
    args.add(getLocationArgument(list, x, y));
    if (list.locationToIndex(new Point(x, y)) == -1) {
      methodName = "actionClick";
    }
    if ((mods != 0 && mods != InputEvent.BUTTON1_DOWN_MASK) || count > 1) {
      methodName = "actionClick";
      args.add(abbot.util.AWT.getMouseModifiers(mods));
      if (count > 1) {
        args.add(String.valueOf(count));
      }
    }
    return new Action(
        getResolver(), null, methodName, args.toArray(new String[0]), javax.swing.JList.class);
  }
}
