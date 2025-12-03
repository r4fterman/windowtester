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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.InputMap;
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

  private static boolean setModifiers(Robot robot, int mask, boolean press) {
    try {
      if ((mask & KeyEvent.SHIFT_DOWN_MASK) != 0) {
        if (press) {
          robot.keyPress(KeyEvent.VK_SHIFT);
        } else {
          robot.keyRelease(KeyEvent.VK_SHIFT);
        }
      }
      if ((mask & KeyEvent.CTRL_DOWN_MASK) != 0) {
        if (press) {
          robot.keyPress(KeyEvent.VK_CONTROL);
        } else {
          robot.keyRelease(KeyEvent.VK_CONTROL);
        }
      }
      if ((mask & KeyEvent.ALT_DOWN_MASK) != 0) {
        if (press) {
          robot.keyPress(KeyEvent.VK_ALT);
        } else {
          robot.keyRelease(KeyEvent.VK_ALT);
        }
      }
      if ((mask & KeyEvent.META_DOWN_MASK) != 0) {
        if (press) {
          robot.keyPress(KeyEvent.VK_META);
        } else {
          robot.keyRelease(KeyEvent.VK_META);
        }
      }
      if ((mask & KeyEvent.ALT_GRAPH_DOWN_MASK) != 0) {
        if (press) {
          robot.keyPress(KeyEvent.VK_ALT_GRAPH);
        } else {
          robot.keyRelease(KeyEvent.VK_ALT_GRAPH);
        }
      }
      return true;
    } catch (IllegalArgumentException e) {
      // ignore these
    } catch (Exception e) {
      Log.warn(e);
    }
    return false;
  }

  private static class KeyWatcher extends KeyAdapter {

    public char keyChar;
    public boolean keyTyped;
    public boolean keyPressed;
    public String codeName;

    public void keyPressed(KeyEvent e) {
      keyPressed = true;
    }

    public void keyTyped(KeyEvent e) {
      keyChar = e.getKeyChar();
      keyTyped = true;
      codeName = null;
    }
  }

  private static KeyWatcher watcher = null;
  private static final int UNTYPED = -1;
  private static final int UNDEFINED = -2;
  private static final int ILLEGAL = -3;
  private static final int SYSTEM = -4;
  private static final int ERROR = -5;

  private static int generateKey(
      final Window w, final Component c, final Robot robot, Point p, String name, int code) {
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
      try {
        watcher.codeName = name;
        watcher.keyTyped = watcher.keyPressed = false;
        robot.keyPress(code);
        robot.keyRelease(code);
        long start = System.currentTimeMillis();
        while (!watcher.keyPressed || !watcher.keyTyped) {
          if (System.currentTimeMillis() - start > 500) {
            break;
          }
          robot.waitForIdle();
        }
        if (!watcher.keyPressed) {
          // alt-tab, alt-f4 and the like which get eaten by the OS
          return SYSTEM;
        } else if (!watcher.keyTyped) {
          // keys which result in KEY_TYPED event
          return UNTYPED;
        } else if (watcher.keyChar == KeyEvent.CHAR_UNDEFINED) {
          // usually the same as UNTYPED, but just in case
          return UNDEFINED;
        } else {
          return watcher.keyChar;
        }
      } catch (IllegalArgumentException e) {
        // not supported on this system
        return ILLEGAL;
      }
    } catch (Exception e) {
      // usually a core library bug
      Log.warn(e);
      return ERROR;
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
          String n1 = o1.getName();
          String n2 = o2.getName();
          return n1.compareTo(n2);
        } catch (Exception e) {
          return 0;
        }
      };

  // From a VK_ code + modifiers, produce a simulated KEY_TYPED
  // From a keychar, determine the necessary VK_ code + modifiers
  private static void generateKeyStrokeMap(Window w, JTextComponent c) {
    // TODO: invoke modifiers for multi-byte input sequences?
    // Skip known modifiers and locking keys
    Collection<String> skip =
        Arrays.asList(
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

    try {
      Robot robot = new Robot();
      // Make sure the window is ready for input
      if (!RobotVerifier.verify(robot)) {
        System.err.println("Robot non-functional, can't generate map");
        System.exit(1);
      }
      robot.delay(500);
      Field[] fields = KeyEvent.class.getDeclaredFields();
      Set<Field> codes = new TreeSet<>(FIELD_COMPARATOR);
      for (Field field : fields) {
        String name = field.getName();
        if (name.startsWith("VK_")
            && !skip.contains(name)
            && !name.startsWith("VK_DEAD_")
            && !isFunctionKey(name)) {
          codes.add(field);
        }
      }
      Point p = c.getLocationOnScreen();
      p.x += c.getWidth() / 2;
      p.y += c.getHeight() / 2;
      // for now, only do reasonable modifiers; add more if the need
      // arises
      int[] modifierCombos = {
        0,
        KeyEvent.SHIFT_DOWN_MASK,
        KeyEvent.CTRL_DOWN_MASK,
        KeyEvent.META_DOWN_MASK,
        KeyEvent.ALT_DOWN_MASK,
        KeyEvent.ALT_GRAPH_DOWN_MASK,
      };

      String[] MODIFIERS = {
        "none", "shift", "control", "meta", "alt", "alt graph",
      };

      // These modifiers might trigger window manager functions
      Map<Field, Object>[] maps = new Map[modifierCombos.length];
      for (int m = 0; m < modifierCombos.length; m++) {
        TreeMap<Field, Object> map = new TreeMap<>(FIELD_COMPARATOR);
        int mask = modifierCombos[m];
        if (!setModifiers(robot, mask, true)) {
          System.out.println("Modifier " + MODIFIERS[m] + " is not currently valid");
          continue;
        }

        Iterator<Field> iter = codes.iterator();
        // Always try to fix the focus; who knows what keys have
        // been mapped to the WM
        boolean focus = true;
        while (iter.hasNext()) {
          Field f = iter.next();
          int code = f.getInt(null);
          System.out.print(".");
          int value = generateKey(w, c, robot, p, f.getName(), code);
          map.put(f, value);
        }
        setModifiers(robot, modifierCombos[m], false);
        System.out.println();
        maps[m] = map;
      }

      Properties props = new Properties();
      for (Field key : maps[0].keySet()) {
        for (int m = 0; m < modifierCombos.length; m++) {
          Map<Field, Object> map = maps[m];
          if (map == null) {
            continue;
          }
          String name = key.getName().substring(3);
          name += "." + Integer.toHexString(modifierCombos[m]);
          int value = (Integer) map.get(key);
          String hex =
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
      String[] names = getMapNames();
      String[] desc = getMapDescriptions();
      for (int i = 0; i < names.length; i++) {
        String fn = getFilename(names[i]);
        FileOutputStream fos = new FileOutputStream(fn);
        props.store(fos, "Key mappings for " + desc[i]);
      }
    } catch (AWTException e) {
      System.err.println("Robot not available, can't generate map");
    } catch (Exception e) {
      System.err.println("Error: " + e);
    }
  }

  /**
   * Run this to generate the full set of mappings for a given locale.
   */
  public static void main(String[] args) {
    String language = System.getProperty("abbot.locale.language");
    if (language != null) {
      String country = System.getProperty("abbot.locale.country", "");
      String variant = System.getProperty("abbot.locale.variant", "");
      Locale.setDefault(Locale.of(language, country, variant));
    }

    final JFrame frame = new JFrame("KeyStroke mapping generator");
    final JTextArea text = new JTextArea();
    // Remove all action mappings; we want to receive *all* keystrokes
    text.setInputMap(JTextArea.WHEN_FOCUSED, new InputMap());
    frame.getContentPane().add(new JScrollPane(text));
    frame.setLocation(100, 100);
    frame.setSize(250, 90);
    frame.addWindowListener(
        new WindowAdapter() {
          public void windowClosing(final WindowEvent e) {
            SwingUtilities.invokeLater(() -> e.getWindow().setVisible(true));
          }
        });
    frame.setVisible(true);
    SwingUtilities.invokeLater(
        () ->
            new Thread("keymap generator") {
              public void run() {
                generateKeyStrokeMap(frame, text);
                System.exit(0);
              }
            }.start());
  }
}
