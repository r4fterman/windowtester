package abbot.editor.widgets;

import abbot.Log;
import abbot.Platform;
import abbot.i18n.Strings;
import abbot.tester.KeyStrokeMap;
import abbot.util.AWT;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

/**
 * Provide access to mnemonics appropriate for the current platform and locale.  Encapsulates
 * displayed text, a KeyEvent.VK_ constant, and a displayed mnemonic index.  All instances are
 * obtained through the factory method, {@link #getMnemonic(String)}.
 *
 * @see java.awt.event.KeyEvent
 * @see javax.swing.AbstractButton#setMnemonic(int)
 * @see javax.swing.AbstractButton#setDisplayedMnemonicIndex(int)
 * @see javax.swing.JLabel#setDisplayedMnemonic(int)
 * @see javax.swing.JLabel#setDisplayedMnemonicIndex(int)
 * @see javax.swing.Action#MNEMONIC_KEY
 */
public class Mnemonic {

  /**
   * The unencoded text.  For example, "&amp;File" results in "File".
   */
  public String text;

  /**
   * The keycode to use as an argument to {@link AbstractButton#setMnemonic(int)}.  Returns
   * KeyEvent.VK_UNDEFINED if no mnemonic was found.
   */
  public int keycode;

  /**
   * The index to use as an argument to {@link AbstractButton#setDisplayedMnemonicIndex(int)}.
   * Returns -1 if the default value should be used.
   */
  public int index;

  private Mnemonic(String text, int keycode, int index) {
    this.text = text;
    this.keycode = keycode;
    this.index = index;
  }

  public static void setDisplayedMnemonicIndex(Component c, int index) {
    if (index == -1) {
      return;
    }
    try {
      Method m = c.getClass().getMethod("setDisplayedMnemonicIndex", int.class);
      m.invoke(c, index);
    } catch (Exception e) {
      // ignore errors
    }
  }

  public String toString() {
    return "Mnemonic text="
        + text
        + ", keycode="
        + AWT.getKeyCode(keycode)
        + (index != -1 ? ("displayed index=" + index) : "");
  }

  public void setMnemonic(AbstractButton button) {
    button.setText(text);
    button.setMnemonic(keycode);
    setDisplayedMnemonicIndex(button, index);
  }

  public void setMnemonic(JLabel label) {
    label.setText(text);
    label.setDisplayedMnemonic(keycode);
    setDisplayedMnemonicIndex(label, index);
  }

  public void setMnemonic(JTabbedPane tabbedPane, int tabIndex) {
    tabbedPane.setTitleAt(tabIndex, text);
    // NOTE: 1.4-only
    try {
      Method m = JTabbedPane.class.getMethod("setMnemonicAt", int.class, int.class);
      m.invoke(tabbedPane, tabIndex, keycode);
      m = JTabbedPane.class.getMethod("setDisplayedMnemonicIndexAt", int.class, int.class);
      if (index != -1) {
        m.invoke(tabbedPane, tabIndex, index);
      }
    } catch (Exception e) {
      // ignore errors
    }
  }

  public void setMnemonic(Action action) {
    action.putValue(Action.NAME, text);
    if (keycode != KeyEvent.VK_UNDEFINED) {
      action.putValue(Action.MNEMONIC_KEY, keycode);
    }
  }

  /**
   * Return whether the character is disallowed as a mnemonic.
   */
  private static boolean isDisallowed(char ch) {
    return Character.isWhitespace(ch) || ch == '\'' || ch == '"';
  }

  /**
   * Return the appropriate mnemonic for the given character.
   */
  private static int getMnemonicMapping(char ch) {
    if (isDisallowed(ch)) {
      return KeyEvent.VK_UNDEFINED;
    }

    if (ch >= 'A' && ch <= 'Z') {
      return KeyEvent.VK_A + ch - 'A';
    }
    if (ch >= 'a' && ch <= 'z') {
      return KeyEvent.VK_A + ch - 'a';
    }
    if (ch >= '0' && ch <= '9') {
      return KeyEvent.VK_0 + ch - '0';
    }

    // See if there's been a mapping defined; usage is similar to NetBeans
    // handling, except that raw integers are not allowed (use the VK_
    // constant name instead).
    String str = Strings.get("MNEMONIC_" + ch, true);
    if (str != null) {
      try {
        return AWT.getKeyCode("VK_" + str.toUpperCase());
      } catch (IllegalArgumentException e) {
        Log.warn("'" + str + "' is not a valid mnemonic " + "(use a VK_ constant from KeyEvent)");
      }
    }

    // Make a guess based on keymaps
    KeyStroke keystroke = KeyStrokeMap.getKeyStroke(ch);
    if (keystroke != null) {
      return keystroke.getKeyCode();
    }

    return KeyEvent.VK_UNDEFINED;
  }

  public static Mnemonic getMnemonic(String input) {
    String text = input;
    int mnemonicIndex = -1;
    int keycode = KeyEvent.VK_UNDEFINED;
    int amp = text.indexOf("&");
    int displayIndex = -1;
    while (amp != -1 && amp < text.length() - 1) {
      char ch = text.charAt(amp + 1);
      if (ch == '&') {
        text = text.substring(0, amp) + text.substring(amp + 1);
        amp = text.indexOf("&", amp + 1);
      } else {
        int code = getMnemonicMapping(ch);
        if (code == KeyEvent.VK_UNDEFINED) {
          amp = text.indexOf("&", amp + 2);
        } else {
          // Only use the first mapping
          if (mnemonicIndex == -1) {
            text = text.substring(0, amp) + text.substring(amp + 1);
            displayIndex = mnemonicIndex = amp;
            keycode = code;
          }
          amp = text.indexOf("&", amp + 1);
        }
      }
    }
    // Mnemonics are not used on OSX
    if (Platform.isOSX()) {
      keycode = KeyEvent.VK_UNDEFINED;
      displayIndex = -1;
    }
    Mnemonic m = new Mnemonic(text, keycode, displayIndex);
    Log.debug(input + "->" + m);
    return m;
  }
}
