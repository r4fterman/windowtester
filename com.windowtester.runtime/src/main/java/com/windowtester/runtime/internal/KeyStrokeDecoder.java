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
package com.windowtester.runtime.internal;

import com.windowtester.runtime.WT;
import java.util.ArrayList;
import java.util.List;

/**
 * Compound keystroke decoder utility.
 */
public class KeyStrokeDecoder {

  // NOTE: made public for testing
  // TODO: this should really have them all!
  // keys
  public static int[] KEY_CONSTANTS = {
    WT.ARROW_DOWN,
    WT.ARROW_UP,
    WT.ARROW_LEFT,
    WT.ARROW_RIGHT,
    WT.ESC,
    WT.TAB,
    WT.CR,
    WT.F1,
    WT.F2,
    WT.F3,
    WT.F4,
    WT.F5,
    WT.F6,
    WT.F7,
    WT.F8,
    WT.F9,
    WT.F10,
    WT.F11,
    WT.F12,
    WT.F13,
    WT.F14,
    WT.F15,
    WT.HELP,
    WT.HOME,
    WT.INSERT,
    WT.PAGE_DOWN,
    WT.PAGE_UP,
    WT.PRINT_SCREEN,
    WT.END
  };

  // modifiers
  public static int[] KEY_MODS = {WT.ALT, WT.SHIFT, WT.CTRL, WT.COMMAND};

  public static int[] extractModifiers(int compositeKey) {
    var keys = new ArrayList<Integer>();
    addMods(compositeKey, keys);
    addKeys(compositeKey, keys);
    return toIntArray(keys);
  }

  private static void addMods(int compositeKey, List<Integer> keys) {
    for (int keyMod : KEY_MODS) {
      if ((compositeKey & keyMod) == keyMod) {
        keys.add(keyMod);
      }
    }
  }

  private static void addKeys(int compositeKey, List<Integer> keys) {
    for (int keyConstant : KEY_CONSTANTS) {
      if ((compositeKey | WT.MODIFIER_MASK) == (keyConstant | WT.MODIFIER_MASK)) {
        keys.add(keyConstant);
      }
    }
  }

  private static int[] toIntArray(List<Integer> keys) {
    return keys.stream().mapToInt(Integer::intValue).toArray();
  }
}
