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
package com.windowtester.internal.tester.swing;

import abbot.i18n.Strings;
import abbot.tester.ActionFailedException;
import abbot.tester.JListLocation;
import java.awt.Component;
import java.awt.event.InputEvent;
import javax.swing.JList;

/***
 *  Extend the Abbot JListTester to add multiple selection capability.
 *  Implemented double click functionality
 */
public class JListTester extends abbot.tester.JListTester {

  private static final int SELECTION_RETRY_COUNT = 3;

  private static final int KEYBOARD_MODIFIER_MASK =
      InputEvent.SHIFT_DOWN_MASK
          | InputEvent.CTRL_DOWN_MASK
          | InputEvent.META_DOWN_MASK
          | InputEvent.ALT_DOWN_MASK
          | InputEvent.ALT_GRAPH_DOWN_MASK;

  private static final int OTHER_BUTTON_MASK =
      InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;

  /**
   * Select the first item in the list matching the given String representation of the item.<p>
   * Equivalent to actionSelectRow(c, new JListLocation(item),buttons).
   */
  public void actionSelectItem(Component c, String item, int buttons) {
    actionSelectRow(c, new JListLocation(item), buttons);
  }

  /**
   * Select the first value in the list matching the given String representation of the value.<p>
   * Equivalent to actionSelectRow(component, new JListLocation(value),buttons).
   */
  public void actionSelectValue(Component component, String value, int buttons) {
    actionSelectRow(component, new JListLocation(value), buttons);
  }

  /**
   * Select the given row.  Does nothing if the index is already selected.
   */
  public void actionSelectRow(Component component, JListLocation location, int buttons) {
    var list = (JList<?>) component;
    var index = location.getIndex(list);
    if (index < 0 || index >= list.getModel().getSize()) {
      var msg = Strings.get("tester.JList.invalid_index", new Object[] {index});
      throw new ActionFailedException(msg);
    }

    if (list.getSelectedIndex() != index) {
      super.actionClick(component, location, buttons);
    }
  }

  /**
   * Double-click on the first item  matching the given String representation of the item.<p>
   * Equivalent to double-click on actionSelectRow(component, new JListLocation(item)).
   */
  public void actionMultipleClick(Component component, int clickCount, String item) {
    actionSelectRow(component, clickCount, new JListLocation(item), InputEvent.BUTTON1_DOWN_MASK);
  }

  /**
   * click with mask specified
   */
  public void actionMultipleClick(Component component, int clickCount, String item, int mask) {
    actionSelectRow(component, clickCount, new JListLocation(item), mask);
  }

  /**
   * click on the given row, with the given clickCount
   */
  public void actionSelectRow(
      Component component, int clickCount, JListLocation location, int mask) {
    var list = (JList<?>) component;
    var index = location.getIndex(list);
    if (index < 0 || index >= list.getModel().getSize()) {
      var msg = Strings.get("tester.JList.invalid_index", new Object[] {index});
      throw new ActionFailedException(msg);
    }

    super.actionClick(component, location, mask, clickCount);

    // A plain single left click should leave the target row selected.  Under load (for
    // example on a busy CI server) the AWT Robot occasionally drops the generated mouse
    // event, so the selection is not updated at all.  Only for a simple left click - i.e.
    // without modifier keys that would deliberately extend (SHIFT) or toggle (CTRL/META)
    // the selection - verify the outcome and click again if it did not take.
    if (clickCount == 1 && isPlainLeftClick(mask)) {
      for (var attempt = 0;
          attempt < SELECTION_RETRY_COUNT && list.getSelectedIndex() != index;
          attempt++) {
        super.actionClick(component, location, mask, clickCount);
      }
    }
  }

  /**
   * Answer whether the given mask represents a plain left mouse click, i.e. the left button
   * without any keyboard modifiers (SHIFT/CTRL/META/ALT) or other mouse buttons that would
   * deliberately extend or toggle the list selection.
   */
  private static boolean isPlainLeftClick(int mask) {
    return (mask & InputEvent.BUTTON1_DOWN_MASK) != 0
        && (mask & OTHER_BUTTON_MASK) == 0
        && (mask & KEYBOARD_MODIFIER_MASK) == 0;
  }
}
