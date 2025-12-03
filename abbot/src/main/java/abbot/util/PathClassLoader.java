package abbot.util;

import abbot.Platform;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Provide a class loader that loads from a custom path.   Similar to
 * sun.misc.Launcher$AppClassLoader (the usual application class loader), except that it doesn't do
 * the security checks that AppClassLoader does. If path given is null, uses java.class.path.
 */
public class PathClassLoader extends java.net.URLClassLoader {

  private final String classPath;
  private static final Factory factory = new Factory();

  public PathClassLoader(String path) {
    this(path, null);
  }

  public PathClassLoader(String path, ClassLoader parent) {
    super(getURLs(path != null ? path : System.getProperty("java.class.path")), parent, factory);
    this.classPath = path != null ? path : System.getProperty("java.class.path");
  }

  public String getClassPath() {
    return classPath;
  }

  /**
   * Returns an array of URLs based on the given classpath string.
   */
  static URL[] getURLs(String p) {
    String s = p != null ? p : System.getProperty("java.class.path");
    File[] files = s != null ? convertPathToFiles(s) : new File[0];
    URL[] urls = new URL[files.length];
    for (int i = 0; i < urls.length; i++) {
      try {
        urls[i] = files[i].toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e.getMessage());
      }
    }
    return urls;
  }

  public static String[] convertPathToFilenames(String path) {
    return convertPathToFilenames(path, ":;");
  }

  public static File[] convertPathToFiles(String path) {
    String[] names = convertPathToFilenames(path, ":;");
    List<File> files = new ArrayList<>();
    for (String name : names) {
      files.add(new File(name));
    }
    return files.toArray(new File[0]);
  }

  private static String[] convertPathToFilenames(String path, String seps) {
    if (path == null) {
      path = "";
    }
    boolean fixDrives = Platform.isWindows() && seps.contains(":");
    StringTokenizer st = new StringTokenizer(path, seps);
    List<String> names = new ArrayList<>();
    while (st.hasMoreTokens()) {
      String fp = st.nextToken();
      // Fix up w32 absolute path names
      if (fixDrives && fp.length() == 1 && st.hasMoreTokens()) {
        char ch = fp.charAt(0);
        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
          fp += ":" + st.nextToken();
        }
      }
      names.add(fp);
    }
    return names.toArray(new String[0]);
  }

  /**
   * Taken from sun.misc.Launcher.
   */
  private static class Factory implements URLStreamHandlerFactory {

    private static final String PREFIX = "sun.net.www.protocol";

    private Factory() {}

    public URLStreamHandler createURLStreamHandler(String protocol) {
      String name = PREFIX + "." + protocol + ".Handler";
      try {
        Class<?> c = Class.forName(name);
        return (URLStreamHandler) c.getDeclaredConstructor().newInstance();
      } catch (ClassNotFoundException
          | NoSuchMethodException
          | InvocationTargetException
          | IllegalAccessException
          | InstantiationException e) {
        e.printStackTrace();
      }
      throw new Error("could not load " + protocol + "system protocol handler");
    }
  }

  public String toString() {
    return super.toString() + " (classpath=" + classPath + ")";
  }
}
