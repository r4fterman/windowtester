package abbot.tester;

import abbot.Log;
import abbot.Platform;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * Provides read/write of locale-specific mappings for virtual keycode-based KeyStrokes to
 * characters and vice versa.
 * <p>
 * If your locale's map is not present in src/abbot/tester/keymaps, please run this class's
 * {@link #main(String[])} method to generate them and
 * <a href="http://sourceforge.net/tracker/?group_id=50939&atid=461492">submit
 * them to the project</a> for inclusion.
 * <p>
 * Variations among locales and OSes are expected; if a map for a locale+OS is not found, the system
 * falls back to the locale map.
 */
public class MapGenerator extends KeyStrokeMap {

  // TODO: invoke modifiers for multi-byte input sequences?
  // Skip known modifiers and locking keys
  private static final List<String> skip =
      List.of(
          "VK_UNDEFINED",
          // modifiers
          "VK_SHIFT",
          "VK_CONTROL",
          "VK_META",
          "VK_ALT",
          "VK_ALT_GRAPH",
          // special-function keys
          "VK_CAPS_LOCK",
          "VK_NUM_LOCK",
          "VK_SCROLL_LOCK",
          // Misc other function keys
          "VK_KANA",
          "VK_KANJI",
          "VK_ALPHANUMERIC",
          "VK_KATAKANA",
          "VK_HIRAGANA",
          "VK_FULL_WIDTH",
          "VK_HALF_WIDTH",
          "VK_ROMAN_CHARACTERS",
          "VK_ALL_CANDIDATES",
          "VK_PREVIOUS_CANDIDATE",
          "VK_CODE_INPUT",
          "VK_JAPANESE_KATAKANA",
          "VK_JAPANESE_HIRAGANA",
          "VK_JAPANESE_ROMAN",
          "VK_KANA_LOCK",
          "VK_INPUT_METHOD_ON_OFF");

  /**
   * Run this to generate the full set of mappings for a given locale.
   */
  public static void main(String[] args) {
    var language = System.getProperty("abbot.locale.language");
    if (language != null) {
      var country = System.getProperty("abbot.locale.country", "");
      var variant = System.getProperty("abbot.locale.variant", "");
      Locale.setDefault(Locale.of(language, country, variant));
    }

    var frame = new JFrame("KeyStroke mapping generator");
    var text = new JTextArea();

    // Remove all action mappings; we want to receive *all* keystrokes
    text.setInputMap(JComponent.WHEN_FOCUSED, new InputMap());

    frame.getContentPane().add(new JScrollPane(text));
    frame.setLocation(100, 100);
    frame.setSize(250, 90);
    frame.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            SwingUtilities.invokeLater(() -> e.getWindow().setVisible(true));
          }
        });
    frame.setVisible(true);

    SwingUtilities.invokeLater(
        () ->
            new Thread("keymap generator") {
              @Override
              public void run() {
                generateKeyStrokeMap(frame, text);
                System.exit(0);
              }
            }.start());
  }

  private static boolean setModifiers(Robot robot, int mask, boolean press) {
    try {
      applyShiftKey(robot, mask, press);
      applyCtrlKey(robot, mask, press);
      applyAltKey(robot, mask, press);
      applyMetaKey(robot, mask, press);
      applyAltGraphKey(robot, mask, press);
      return true;
    } catch (IllegalArgumentException e) {
      // ignore these
    } catch (Exception e) {
      Log.warn(e);
    }
    return false;
  }

  private static void applyAltGraphKey(Robot robot, int mask, boolean press) {
    if ((mask & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
      if (press) {
        robot.keyPress(KeyEvent.VK_ALT_GRAPH);
      } else {
        robot.keyRelease(KeyEvent.VK_ALT_GRAPH);
      }
    }
  }

  private static void applyMetaKey(Robot robot, int mask, boolean press) {
    if ((mask & InputEvent.META_DOWN_MASK) != 0) {
      if (press) {
        robot.keyPress(KeyEvent.VK_META);
      } else {
        robot.keyRelease(KeyEvent.VK_META);
      }
    }
  }

  private static void applyAltKey(Robot robot, int mask, boolean press) {
    if ((mask & InputEvent.ALT_DOWN_MASK) != 0) {
      if (press) {
        robot.keyPress(KeyEvent.VK_ALT);
      } else {
        robot.keyRelease(KeyEvent.VK_ALT);
      }
    }
  }

  private static void applyCtrlKey(Robot robot, int mask, boolean press) {
    if ((mask & InputEvent.CTRL_DOWN_MASK) != 0) {
      if (press) {
        robot.keyPress(KeyEvent.VK_CONTROL);
      } else {
        robot.keyRelease(KeyEvent.VK_CONTROL);
      }
    }
  }

  private static void applyShiftKey(Robot robot, int mask, boolean press) {
    if ((mask & InputEvent.SHIFT_DOWN_MASK) != 0) {
      if (press) {
        robot.keyPress(KeyEvent.VK_SHIFT);
      } else {
        robot.keyRelease(KeyEvent.VK_SHIFT);
      }
    }
  }

  private static class KeyWatcher extends KeyAdapter {
    private char keyChar;
    private boolean keyTyped;

    private boolean keyPressed;

    @Override
    public void keyPressed(KeyEvent e) {
      keyPressed = true;
    }

    @Override
    public void keyTyped(KeyEvent e) {
      keyChar = e.getKeyChar();
      keyTyped = true;
    }
  }

  private static KeyWatcher watcher = null;
  private static final int UNTYPED = -1;
  private static final int UNDEFINED = -2;
  private static final int ILLEGAL = -3;
  private static final int SYSTEM = -4;

  private static final int ERROR = -5;

  private static int generateKey(Window w, Component c, Robot robot, Point p, int code) {
    if (watcher == null) {
      watcher = new KeyWatcher();
      c.addKeyListener(watcher);
    }
    try {
      robot.waitForIdle();
      SwingUtilities.invokeAndWait(
          (Runnable)
              () -> {
                w.setVisible(true);
                w.toFront();
                c.requestFocus();
                if (Platform.isWindows() || Platform.isMacintosh()) {
                  robot.mouseMove(w.getX() + w.getWidth() / 2, w.getY() + w.getHeight() / 2);
                  robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                  robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                }
              });
      robot.mouseMove(p.x, p.y);
      robot.waitForIdle();
      return generateKey(robot, code);
    } catch (Exception e) {
      Log.warn(e);
      return ERROR;
    }
  }

  private static int generateKey(Robot robot, int code) {
    try {
      watcher.keyTyped = false;
      watcher.keyPressed = false;

      robot.keyPress(code);
      robot.keyRelease(code);

      var start = System.currentTimeMillis();
      while (!watcher.keyPressed || !watcher.keyTyped) {
        if (System.currentTimeMillis() - start > 500) {
          break;
        }
        robot.waitForIdle();
      }

      if (!watcher.keyPressed) {
        // alt-tab, alt-f4 and the like that get eaten by the OS
        return SYSTEM;
      }
      if (!watcher.keyTyped) {
        // keys that result in KEY_TYPED event
        return UNTYPED;
      }
      if (watcher.keyChar == KeyEvent.CHAR_UNDEFINED) {
        // usually the same as UNTYPED, but just in case
        return UNDEFINED;
      }
      return watcher.keyChar;
    } catch (IllegalArgumentException e) {
      // not supported on this system
      return ILLEGAL;
    }
  }

  private static boolean isFunctionKey(String name) {
    if (name.startsWith("VK_F")) {
      try {
        Integer.parseInt(name.substring(4));
        return true;
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    return false;
  }

  private static final Comparator<Field> FIELD_COMPARATOR =
      (o1, o2) -> {
        try {
          var n1 = o1.getName();
          var n2 = o2.getName();
          return n1.compareTo(n2);
        } catch (Exception e) {
          return 0;
        }
      };

  // From a VK_ code + modifiers, produce a simulated KEY_TYPED
  // From a key char, determine the necessary VK_ code + modifiers

  private static void generateKeyStrokeMap(Window w, JTextComponent c) {
    logInfo("Generating keystroke map");
    try {
      var robot = new Robot();

      // Make sure the window is ready for input
      if (!RobotVerifier.verify(robot)) {
        logError("Robot non-functional, can't generate map");
        System.exit(1);
      }
      robot.delay(500);

      var fields = KeyEvent.class.getDeclaredFields();
      var codes = new TreeSet<>(FIELD_COMPARATOR);
      for (Field field : fields) {
        var name = field.getName();
        if (name.startsWith("VK_")
            && !skip.contains(name)
            && !name.startsWith("VK_DEAD_")
            && !isFunctionKey(name)) {
          codes.add(field);
        }
      }

      logInfo("Total VK_ fields read: " + codes.size());
      var p = c.getLocationOnScreen();
      p.x += c.getWidth() / 2;
      p.y += c.getHeight() / 2;

      // for now, only do reasonable modifiers; add more if the need arises
      var modifierCombos =
          List.of(
              0,
              InputEvent.SHIFT_DOWN_MASK,
              InputEvent.CTRL_DOWN_MASK,
              InputEvent.META_DOWN_MASK,
              InputEvent.ALT_DOWN_MASK,
              InputEvent.ALT_GRAPH_DOWN_MASK);

      var modifiers =
          new String[] {
            "none", "shift", "control", "meta", "alt", "alt graph",
          };

      var maps = new ArrayList<Map<Field, Integer>>(modifierCombos.size());
      for (int m = 0; m < modifierCombos.size(); m++) {
        var map = new TreeMap<Field, Integer>(FIELD_COMPARATOR);
        var mask = modifierCombos.get(m);
        if (!setModifiers(robot, mask, true)) {
          logInfo("Modifier " + modifiers[m] + " is not currently valid");
          continue;
        }
        logInfo("Generating keys with mask=" + modifiers[m]);

        // Always try to fix the focus; who knows what keys have been mapped to the WM
        for (Field f : codes) {
          var code = f.getInt(null);
          printProgress();
          var value = generateKey(w, c, robot, p, code);
          map.put(f, value);
        }

        setModifiers(robot, modifierCombos.get(m), false);
        logInfo("");

        maps.add(map);
      }

      var props = new Properties();
      for (Field key : maps.getFirst().keySet()) {
        for (int m = 0; m < modifierCombos.size(); m++) {
          var map = maps.get(m);
          if (map == null) {
            continue;
          }
          var name = key.getName().substring(3) + "." + Integer.toHexString(modifierCombos.get(m));
          var value = map.get(key);

          var hex =
              switch (value) {
                case UNTYPED -> "untyped";
                case UNDEFINED -> "undefined";
                case ILLEGAL -> "illegal";
                case SYSTEM -> "system";
                case ERROR -> "error";
                default -> Integer.toHexString(value);
              };
          props.setProperty(name, hex);
        }
      }

      var names = getMapNames();
      var desc = getMapDescriptions();
      for (int i = 0; i < names.length; i++) {
        var filename = getFilename(names[i]);
        logInfo("Saving " + names[i] + " as " + filename);
        try (FileOutputStream fos = new FileOutputStream(filename)) {
          props.store(fos, "Key mappings for " + desc[i]);
        }
      }
    } catch (AWTException e) {
      logError("Robot not available, can't generate map");
    } catch (Exception e) {
      logError("Error: " + e);
    }
  }

  private static void printProgress() {
    System.out.print(".");
  }

  private static void logInfo(String message) {
    System.out.println(message);
  }

  private static void logError(String message) {
    System.err.println(message);
  }
}
