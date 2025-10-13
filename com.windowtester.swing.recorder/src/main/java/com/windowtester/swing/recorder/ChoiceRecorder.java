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
import java.awt.Choice;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

/**
 * Record basic semantic events you might find on an Choice component. <p>
 */
public class ChoiceRecorder extends ComponentRecorder {

  private Choice choice = null;

  /**
   * If selection is null when finished, no step will be generated.
   */
  private String selection = null;

  private ItemListener listener = null;

  public ChoiceRecorder(Resolver resolver) {
    super(resolver);
  }

  @Override
  protected void init(int recordingType) {
    super.init(recordingType);
    choice = null;
    selection = null;
    listener = null;
  }

  @Override
  protected boolean isClick(AWTEvent e) {
    if (e instanceof ItemEvent) {
      return true;
    }
    return super.isClick(e);
  }

  @Override
  protected boolean parseClick(AWTEvent event) {
    // Have to check here since we handle the ItemEvent artificially
    if (isFinished()) {
      Log.debug("already finished");
      return false;
    }

    if (choice == null) {
      // Parse immediate selections (programmatic/tester driven)
      if (event instanceof ItemEvent) {
        choice = (Choice) event.getSource();
        selection = ((ItemEvent) event).getItem().toString();
        Log.debug("selection=" + selection);
        setFinished(true);
      } else {
        choice = (Choice) event.getSource();
        listener =
            new ItemListener() {
              @Override
              public void itemStateChanged(ItemEvent ev) {
                Log.debug("item event");
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                  selection = ev.getItem().toString();
                  Log.debug("selection=" + selection);
                  choice.removeItemListener(this);
                  setFinished(true);
                }
              }
            };
        choice.addItemListener(listener);
        setStatus("Waiting for selection");
      }
    } else if (event.getID() == KeyEvent.KEY_RELEASED
        && (((KeyEvent) event).getKeyCode() == KeyEvent.VK_SPACE
            || ((KeyEvent) event).getKeyCode() == KeyEvent.VK_ENTER)) {
      Log.debug("enter");
      setFinished(true);
    } else if (event.getID() == KeyEvent.KEY_RELEASED
        && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_ESCAPE) {
      Log.debug("cancel");
      selection = null;
      setFinished(true);
    } else {
      Log.debug("Event ignored");
    }

    if (isFinished() && choice != null) {
      choice.removeItemListener(listener);
      listener = null;
    }

    // Events are always consumed by the Choice
    return true;
  }

  @Override
  protected Step createStep() {
    Step step = null;
    if (getRecordingType() == SE_CLICK) {
      if (selection != null) {
        step = createSelection(choice, selection);
      }
    } else {
      step = super.createStep();
    }
    return step;
  }

  protected Step createSelection(Choice target, String selection) {
    // TODO : add windowtester event generation
    ComponentReference cr = getResolver().addComponent(choice);
    return new Action(
        getResolver(),
        null,
        "actionSelectItem",
        new String[] {cr.getID(), selection},
        java.awt.Choice.class);
  }
}
