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
package com.windowtester.internal.swing.util;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for decoding keystroke for keyclicks
 */
public class KeyStrokeDecoder {

  private static final int[] KEY_CONSTANTS = {
    KeyEvent.VK_DOWN,
    KeyEvent.VK_UP,
    KeyEvent.VK_LEFT,
    KeyEvent.VK_RIGHT,
    KeyEvent.VK_ESCAPE,
    KeyEvent.VK_TAB,
    KeyEvent.VK_ENTER,
    KeyEvent.VK_F1,
    KeyEvent.VK_F2,
    KeyEvent.VK_F3,
    KeyEvent.VK_F4,
    KeyEvent.VK_F5,
    KeyEvent.VK_F6,
    KeyEvent.VK_F7,
    KeyEvent.VK_F8,
    KeyEvent.VK_F9,
    KeyEvent.VK_F10,
    KeyEvent.VK_F11,
    KeyEvent.VK_F12,
    KeyEvent.VK_F13,
    KeyEvent.VK_F14,
    KeyEvent.VK_F15,
    KeyEvent.VK_HELP,
    KeyEvent.VK_HOME,
    KeyEvent.VK_INSERT,
    KeyEvent.VK_PAGE_DOWN,
    KeyEvent.VK_PAGE_UP,
    KeyEvent.VK_PRINTSCREEN,
    KeyEvent.VK_END
  };

  private static final int[] KEY_MODS = {
    InputEvent.ALT_DOWN_MASK,
    InputEvent.SHIFT_DOWN_MASK,
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
  };

  private static final int MODIFIER_MASK =
      InputEvent.ALT_DOWN_MASK
          | InputEvent.SHIFT_DOWN_MASK
          | Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

  public static int extractModifiers(int compositeKey) {
    int modifiers = 0;
    int candidate;
    for (int keyMod : KEY_MODS) {
      candidate = keyMod;
      if ((compositeKey & candidate) == candidate) {
        modifiers |= candidate;
      }
    }
    return modifiers;
  }

  public static int[] extractKeys(int compositeKey) {
    var keys = new ArrayList<Integer>();
    for (int keyConstant : KEY_CONSTANTS) {
      if ((compositeKey | MODIFIER_MASK) == (keyConstant | MODIFIER_MASK)) {
        keys.add(keyConstant);
      }
    }
    return toIntArray(keys);
  }

  private static int[] toIntArray(List<Integer> keys) {
    return keys.stream().mapToInt(i -> i).toArray();
  }

  private KeyStrokeDecoder() {
    // do nothing
  }
}
