package abbot;

import java.util.StringTokenizer;

/**
 * Simple utility to figure out what platform we're on, what java version we're running.
 */
public class Platform {

  public static final int JAVA_1_0 = 0x1000;
  public static final int JAVA_1_1 = 0x1100;
  public static final int JAVA_1_2 = 0x1200;
  public static final int JAVA_1_3 = 0x1300;
  public static final int JAVA_1_4 = 0x1400;
  public static final int JAVA_1_5 = 0x1500;

  public static final String OS_NAME;
  public static final String JAVA_VERSION_STRING;
  public static final int JAVA_VERSION;

  static {
    OS_NAME = System.getProperty("os.name");
    JAVA_VERSION_STRING = System.getProperty("java.version");
    JAVA_VERSION = parse(JAVA_VERSION_STRING);
  }

  private static final boolean IS_WINDOWS = OS_NAME.startsWith("Windows");
  private static final boolean IS_WINDOWS_9_X =
      IS_WINDOWS && (OS_NAME.contains("95") || OS_NAME.contains("98") || OS_NAME.contains("ME"));

  private static final boolean IS_WINDOWS_XP = IS_WINDOWS && OS_NAME.contains("XP");
  private static final boolean IS_MAC = System.getProperty("mrj.version") != null;
  private static final boolean IS_OSX = IS_MAC && OS_NAME.contains("OS X");
  private static final boolean IS_SUN_OS =
      OS_NAME.startsWith("SunOS") || OS_NAME.startsWith("Solaris");
  private static final boolean IS_HP_UX = OS_NAME.equals("HP-UX");
  private static final boolean IS_LINUX = OS_NAME.equals("Linux");

  /**
   * No instantiations.
   */
  private Platform() {}

  private static String strip(String number) {
    while (number.startsWith("0") && number.length() > 1) {
      number = number.substring(1);
    }
    return number;
  }

  private static int parse(String potentialVersion) {
    int version = 0;
    try {
      var tokenizer = new StringTokenizer(potentialVersion, "._");
      version = Integer.parseInt(strip(tokenizer.nextToken())) * 0x1000;
      version += Integer.parseInt(strip(tokenizer.nextToken())) * 0x100;
      version += Integer.parseInt(strip(tokenizer.nextToken())) * 0x10;
      version += Integer.parseInt(strip(tokenizer.nextToken()));
    } catch (NumberFormatException | java.util.NoSuchElementException nfe) {
      // ignore
    }
    return version;
  }

  // FIXME this isn't entirely correct, maybe should look for a motif class instead.
  public static boolean isX11() {
    return !IS_OSX && !IS_WINDOWS;
  }

  public static boolean isWindows() {
    return IS_WINDOWS;
  }

  public static boolean isWindows9X() {
    return IS_WINDOWS_9_X;
  }

  public static boolean isWindowsXP() {
    return IS_WINDOWS_XP;
  }

  public static boolean isMacintosh() {
    return IS_MAC;
  }

  public static boolean isOSX() {
    return IS_OSX;
  }

  public static boolean isSolaris() {
    return IS_SUN_OS;
  }

  public static boolean isHPUX() {
    return IS_HP_UX;
  }

  public static boolean isLinux() {
    return IS_LINUX;
  }
}
