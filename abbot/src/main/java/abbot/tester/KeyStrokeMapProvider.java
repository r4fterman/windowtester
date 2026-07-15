package abbot.tester;

import java.util.Map;
import javax.swing.KeyStroke;

/**
 * Provides read/write of local-specific mappings for virtual keycode-based KeyStrokes to characters and vice versa.
 */
public interface KeyStrokeMapProvider {
  /**
   * Returns a map for the current locale which translates an Integer virtual keycode (VK_XXX) into a the Character it
   * produces.  May not necessarily map all keycode/modifier combinations.
   */
  Map<KeyStroke, Character> loadCharacterMap();

  /**
   * Returns a map for the current locale which translates a Character into a keycode-based KeyStroke.  Where multiple
   * keycodes may produce the same Character output, the simplest keystroke is used.
   */
  Map<Character, KeyStroke> loadKeyStrokeMap();
}
