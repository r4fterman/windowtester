package abbot.tester;

import abbot.Log;
import abbot.Platform;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.swing.KeyStroke;

/**
 * Provides read of local-specific mappings for virtual keycode-based KeyStrokes to characters and
 * vice versa. The map format is a properties file with each line containing an entry of the
 * form<br>
 * <code>VKNAME.MOD=VALUE</code><br>
 * The VKNAME is the String suffix of the KeyEvent VK_ keycode.  MOD is the integer value of the
 * current modifier mask (assumes only a single modifier has any effect on key output, interesting
 * values are considered to be 0, 1, 2, 8). VALUE is the char value of the KEY_TYPED keyChar
 * corresponding to the VK_ keycode and modifiers, as an integer value.
 */
public class KeyStrokeMap implements KeyStrokeMapProvider {

  /**
   * Map of Characters to virtual keycode-based KeyStrokes.
   */
  private static final Map<Character, KeyStroke> keycodes = getKeyStrokeMap();

  /**
   * Map of keycode-based KeyStrokes to Characters.
   */
  private static final Map<KeyStroke, Character> chars = getCharacterMap();

  /**
   * Return the keycode-based KeyStroke corresponding to the given character, as best we can guess
   * it, or null if we don't know how to generate it.
   */
  public static KeyStroke getKeyStroke(char ch) {
    return keycodes.get(ch);
  }

  /**
   * Given a keycode-based KeyStroke, return the equivalent character. Defined properly for US
   * keyboards only.  Please contribute your own.
   *
   * @return KeyEvent.VK_UNDEFINED if the result is unknown.
   */
  public static char getChar(KeyStroke ks) {
    Character ch = chars.get(ks);
    if (ch == null) {
      // Try again, but strip all modifiers but shift
      int mask = ks.getModifiers() & ~InputEvent.SHIFT_DOWN_MASK;
      var keyStroke = KeyStroke.getKeyStroke(ks.getKeyCode(), mask);
      ch = chars.get(keyStroke);
      if (ch == null) {
        return KeyEvent.CHAR_UNDEFINED;
      }
    }
    return ch;
  }

  private static KeyStrokeMapProvider generator = null;

  /**
   * If available, provide a dedicated class to provide mappings between keystrokes and generated
   * characters.
   */
  private static KeyStrokeMapProvider getGenerator() {
    if (generator == null) {
      try {
        String gname =
            System.getProperty("abbot.keystroke_map_generator", "abbot.tester.KeyStrokeMap");
        if (gname != null) {
          generator =
              (KeyStrokeMapProvider) Class.forName(gname).getDeclaredConstructor().newInstance();
        }
      } catch (Exception e) {
        Log.warn(e);
      }
    }
    return generator;
  }

  private static Map<KeyStroke, Character> getCharacterMap() {
    KeyStrokeMapProvider generator = getGenerator();
    Map<KeyStroke, Character> m = generator != null ? generator.loadCharacterMap() : null;
    return m != null ? m : generateCharacterMappings();
  }

  /**
   * Generate a map from characters to virtual keycode-based KeyStrokes.
   */
  private static Map<KeyStroke, Character> generateCharacterMappings() {
    Log.debug("Generating default character mappings");
    Map<KeyStroke, Character> map = new HashMap<>();
    for (Map.Entry<Character, KeyStroke> entry : keycodes.entrySet()) {
      map.put(entry.getValue(), entry.getKey());
    }
    return map;
  }

  private static Map<Character, KeyStroke> getKeyStrokeMap() {
    KeyStrokeMapProvider generator = getGenerator();
    Map<Character, KeyStroke> map = generator != null ? generator.loadKeyStrokeMap() : null;
    return map != null ? map : generateKeyStrokeMappings();
  }

  /**
   * Generate the mapping between characters and key codes.   This is invoked exactly once per VM
   * invocation. We don't have complete coverage, so if you use this fallback map in AWT mode some
   * events may be missing that would otherwise be generated in robot mode.
   */
  private static Map<Character, KeyStroke> generateKeyStrokeMappings() {
    Log.debug("Generating default keystroke mappings");
    int shift = InputEvent.SHIFT_DOWN_MASK;
    int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    int[][] universalMappings = {
      {'', KeyEvent.VK_ESCAPE, 0},
      {'\b', KeyEvent.VK_BACK_SPACE, 0},
      {'', KeyEvent.VK_DELETE, 0},
      {'\n', KeyEvent.VK_ENTER, 0},
      {'\r', KeyEvent.VK_ENTER, 0},
    };
    // Add to these as needed; note that this is based on a US keyboard
    // mapping, and will likely fail for others.
    int[][] mappings = {
      {
        ' ', KeyEvent.VK_SPACE, 0,
      },
      {
        '\t', KeyEvent.VK_TAB, 0,
      },
      {
        '~', KeyEvent.VK_BACK_QUOTE, shift,
      },
      {
        '`', KeyEvent.VK_BACK_QUOTE, 0,
      },
      {
        '!', KeyEvent.VK_1, shift,
      },
      {
        '@', KeyEvent.VK_2, shift,
      },
      {
        '#', KeyEvent.VK_3, shift,
      },
      {
        '$', KeyEvent.VK_4, shift,
      },
      {
        '%', KeyEvent.VK_5, shift,
      },
      {
        '^', KeyEvent.VK_6, shift,
      },
      {
        '&', KeyEvent.VK_7, shift,
      },
      {
        '*', KeyEvent.VK_8, shift,
      },
      {
        '(', KeyEvent.VK_9, shift,
      },
      {
        ')', KeyEvent.VK_0, shift,
      },
      {
        '-', KeyEvent.VK_MINUS, 0,
      },
      {
        '_', KeyEvent.VK_MINUS, shift,
      },
      {
        '=', KeyEvent.VK_EQUALS, 0,
      },
      {
        '+', KeyEvent.VK_EQUALS, shift,
      },
      {
        '[', KeyEvent.VK_OPEN_BRACKET, 0,
      },
      {
        '{', KeyEvent.VK_OPEN_BRACKET, shift,
      },
      // NOTE: The following does NOT produce a left brace
      // { '{', KeyEvent.VK_BRACELEFT, 0, },
      {
        ']', KeyEvent.VK_CLOSE_BRACKET, 0,
      },
      {
        '}', KeyEvent.VK_CLOSE_BRACKET, shift,
      },
      {
        '|', KeyEvent.VK_BACK_SLASH, shift,
      },
      {
        ';', KeyEvent.VK_SEMICOLON, 0,
      },
      {
        ':', KeyEvent.VK_SEMICOLON, shift,
      },
      {
        ',', KeyEvent.VK_COMMA, 0,
      },
      {
        '<', KeyEvent.VK_COMMA, shift,
      },
      {
        '.', KeyEvent.VK_PERIOD, 0,
      },
      {
        '>', KeyEvent.VK_PERIOD, shift,
      },
      {
        '/', KeyEvent.VK_SLASH, 0,
      },
      {
        '?', KeyEvent.VK_SLASH, shift,
      },
      {
        '\\', KeyEvent.VK_BACK_SLASH, 0,
      },
      {
        '|', KeyEvent.VK_BACK_SLASH, shift,
      },
      {
        '\'', KeyEvent.VK_QUOTE, 0,
      },
      {
        '"', KeyEvent.VK_QUOTE, shift,
      },
    };

    Map<Character, KeyStroke> map = new HashMap<>();
    // Universal mappings
    for (int[] entry : universalMappings) {
      KeyStroke stroke = KeyStroke.getKeyStroke(entry[1], entry[2]);
      map.put((char) entry[0], stroke);
    }

    // If the locale is not en_US/GB, provide only a very basic map and
    // rely on key_typed events instead
    Locale locale = Locale.getDefault();
    if (!Locale.US.equals(locale) && !Locale.UK.equals(locale)) {
      Log.debug("Not US: " + locale);
      return map;
    }

    // Basic symbol/punctuation mappings
    for (int[] entry : mappings) {
      KeyStroke stroke = KeyStroke.getKeyStroke(entry[1], entry[2]);
      map.put((char) entry[0], stroke);
    }

    // Lowercase
    for (int i = 'a'; i <= 'z'; i++) {
      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_A + i - 'a', 0);
      map.put((char) i, stroke);
      // control characters
      stroke = KeyStroke.getKeyStroke(KeyEvent.VK_A + i - 'a', ctrl);
      Character key = (char) (i - 'a' + 1);
      // Make sure we don't overwrite something already there
      map.putIfAbsent(key, stroke);
    }

    // Capitals
    for (int i = 'A'; i <= 'Z'; i++) {
      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_A + i - 'A', shift);
      map.put((char) i, stroke);
    }
    // digits
    for (int i = '0'; i <= '9'; i++) {
      KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_0 + i - '0', 0);
      map.put((char) i, stroke);
    }
    return map;
  }

  private static Map<KeyStroke, Character> characterMap = null;
  private static Map<Character, KeyStroke> keyStrokeMap = null;
  private static boolean loaded = false;

  private static InputStream findMap() {
    String[] names = getMapNames();
    for (String s : names) {
      Log.debug("Trying " + s);
      String name = getFilename(s);
      InputStream is = KeyStrokeMapProvider.class.getResourceAsStream("keymaps/" + name);
      if (is != null) {
        return is;
      }
    }
    return KeyStrokeMapProvider.class.getResourceAsStream("keymaps/default.map");
  }

  private synchronized void loadMaps() {
    if (loaded) {
      return;
    }
    Map<KeyStroke, Character> cmap = new HashMap<>();
    Map<Character, KeyStroke> kmap = new HashMap<>();

    Properties props = new Properties();
    try {
      InputStream is = findMap();
      if (is == null) {
        Log.debug("No appropriate map file found");
        loaded = true;
        return;
      }
      props.load(is);

      for (Object o : props.keySet()) {
        String key = (String) o;
        Log.debug("Property " + key + "=" + props.getProperty(key));
        try {
          String codeName = key.substring(0, key.indexOf("."));
          int mask = Integer.parseInt(key.substring(key.indexOf(".") + 1), 16);
          int value = Integer.parseInt(props.getProperty(key), 16);
          Character ch = (char) value;
          Field field = KeyEvent.class.getField("VK_" + codeName);
          int code = field.getInt(null);
          KeyStroke ks = KeyStroke.getKeyStroke(code, mask);
          // May be more than one KeyStroke mapping to a given key
          // character; prefer no mask or shift mask over any other
          // masks.
          KeyStroke existing = kmap.get(ch);
          if (existing == null
              || ((existing.getModifiers() != 0
                      && existing.getModifiers() != KeyEvent.SHIFT_DOWN_MASK)
                  || (mask == 0
                      && (existing.getModifiers() != 0
                          || ks.toString().length() < existing.toString().length())))) {
            Log.debug("Installing " + ks + " for '" + ch + "'");
            kmap.put(ch, ks);
          }
          cmap.put(ks, ch);
        } catch (NumberFormatException e) {
          // ignore invalid entries
        } catch (Exception e) {
          Log.warn(e);
        }
      }
    } catch (IOException io) {
      // ignore
    }
    Log.debug("Successfully loaded character/keystroke map");
    characterMap = cmap;
    keyStrokeMap = kmap;
    loaded = true;
  }

  /**
   * Load a map for the current locale to translate a character into a corresponding virtual
   * keycode-based KeyStroke.
   */
  public Map<KeyStroke, Character> loadCharacterMap() {
    loadMaps();
    return characterMap;
  }

  /**
   * Load a map for the current locale to translate a virtual keycode into a character-based
   * KeyStroke.
   */
  public Map<Character, KeyStroke> loadKeyStrokeMap() {
    loadMaps();
    return keyStrokeMap;
  }

  /**
   * Convert a String containing a unique identifier for the map into a unique filename.
   */
  protected static String getFilename(String base) {
    return base + ".map";
  }

  protected static String[] getMapNames() {
    return getMapStrings(false);
  }

  protected static String[] getMapDescriptions() {
    return getMapStrings(true);
  }

  /**
   * Return the keystroke map filenames that should be available for this locale/OS/VM
   * version/architecture.  Assume most changes across locale, then OS, then VM version, then os
   * version/architecture.
   */
  private static String[] getMapStrings(boolean desc) {
    List<String> list = new ArrayList<>();
    Locale locale = Locale.getDefault();
    String name = locale.toString();
    if (desc) {
      name = "locale=" + name;
    }
    list.addFirst(name);

    String os = "-" + getOSType();
    if (desc) {
      os = " (os=" + System.getProperty("os.name") + ", " + System.getProperty("os.version") + ")";
    }
    name += os;
    list.addFirst(name);
    return list.toArray(new String[0]);
  }

  private static String getOSType() {
    var osType = Platform.isWindows() ? "w32" : "x11";
    return Platform.isMacintosh() ? "mac" : osType;
  }
}
