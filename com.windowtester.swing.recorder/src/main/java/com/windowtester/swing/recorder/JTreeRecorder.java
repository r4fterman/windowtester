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
import abbot.tester.JTreeTester;
import com.windowtester.recorder.event.IUISemanticEvent;
import com.windowtester.recorder.event.UISemanticEventFactory;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JTree;

/**
 * Record basic semantic events you might find on a JTree. <p>
 * <ul>
 * <li>Click one or more times in a cell
 * </ul>
 * Added windowtester semantic event generation
 */
public class JTreeRecorder extends JComponentRecorder {

  public JTreeRecorder(Resolver resolver) {
    super(resolver);
  }

  @Override
  protected Step createClick(Component target, int x, int y, int mods, int count) {
    String mask = null;

    JTree tree = (JTree) target;
    ComponentReference cr = getResolver().addComponent(target);
    String methodName = "actionSelectRow";

    List<String> args = new ArrayList<>();
    args.add(cr.getID());
    args.add(getLocationArgument(target, x, y));
    if (tree.getRowForLocation(x, y) == -1) {
      if (JTreeTester.isLocationInExpandControl(tree, x, y) && count == 1) {
        methodName = "actionToggleRow";
      } else {
        methodName = "actionClick";
      }
    }

    if ((mods != 0 && mods != InputEvent.BUTTON1_DOWN_MASK) || count > 1) {
      // using methodName as an indication for generation
      // of windowtester semantic events
      // methodName = "actionClick";
      mask = abbot.util.AWT.getMouseModifiers(mods);
      args.add(mask);
      if (count > 1) {
        args.add(String.valueOf(count));
      }
    }
    // create semantic event
    if (!methodName.equals("actionClick")) {
      IUISemanticEvent semanticEvent =
          UISemanticEventFactory.createTreeItemSelectionEvent(
              (JTree) target, x, y, mask, count, getButton());
      notify(semanticEvent);
    }

    return new Action(
        getResolver(), null, methodName, args.toArray(new String[0]), javax.swing.JTree.class);
  }

  @Override
  protected Step createPopupMenuSelection(Component invoker, int x, int y, Component menuItem) {

    IUISemanticEvent semanticEvent =
        UISemanticEventFactory.createTreeItemContextMenuSelectionEvent(
            (JTree) invoker, x, y, (JMenuItem) menuItem);
    notify(semanticEvent);
    // semantic event has been generated
    doneEventGeneration = true;
    return super.createPopupMenuSelection(invoker, x, y, menuItem);
  }
}
