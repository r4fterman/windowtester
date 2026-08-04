package abbot.script;

import abbot.Log;
import abbot.WaitTimedOutError;
import abbot.finder.ComponentNotFoundException;
import abbot.finder.ComponentSearchException;
import abbot.finder.MultipleComponentsFoundException;
import abbot.i18n.Strings;
import abbot.script.parsers.Parser;
import abbot.tester.ComponentTester;
import java.awt.Component;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provide parsing of a String into an array of appropriately typed arguments.   Arrays are
 * indicated by square brackets, and arguments are separated by commas, e.g.<br>
 * <ul>
 * <li>An empty String array (length zero): "[]"
 * <li>Three arguments "one,two,three"
 * <li>An array of length three: "[one,two,three]"
 * <li>A single-element array of integer: "[1]"
 * <li>A single null argument: "null"
 * <li>An array of two strings: "[one,two]"
 * <li>Commas must be escaped when they would otherwise be interpreted as an
 * argument separator:<br>
 * "one,two%2ctwo,three" (the 2nd argument is "two,two")
 * </ul>
 */
public class ArgumentParser {

  private ArgumentParser() {}

  private static final String ESC_ESC_COMMA = "%%2C";
  public static final String ESC_COMMA = "%2c";
  public static final String NULL = "null";
  public static final String DEFAULT_TOSTRING = "<default-tostring>";

  /**
   * Maps class names to their corresponding string parsers.
   */
  private static final Map<Class<?>, Parser> parsers = new HashMap<>();

  private static boolean isExtension(String name) {
    return name.contains(".extensions.");
  }

  private static Parser findParser(String name, Class<?> targetClass) {
    Log.debug("Trying " + name + " for " + targetClass);
    try {
      Class<?> cvtClass =
          isExtension(name)
              ? Class.forName(name, true, targetClass.getClassLoader())
              : Class.forName(name);
      Parser parser = (Parser) cvtClass.getDeclaredConstructor().newInstance();
      if (!cvtClass.getName().contains(".extensions.")) {
        parsers.put(targetClass, parser);
      }
      return parser;
    } catch (InstantiationException
        | IllegalAccessException
        | ClassNotFoundException
        | NoSuchMethodException
        | InvocationTargetException e) {
      Log.debug(e);
    }
    return null;
  }

  public static Parser setParser(Class<?> cls, Parser parser) {
    Parser old = parsers.get(cls);
    parsers.put(cls, parser);
    return old;
  }

  public static Parser getParser(Class<?> cls) {
    Parser parser = parsers.get(cls);
    // Load core testers with the current framework's class loader
    // context, and anything else in the context of the code under test
    if (parser == null) {
      String base = ComponentTester.simpleClassName(cls);
      String pkg = Parser.class.getPackage().getName();
      parser = findParser(pkg + "." + base + "Parser", cls);
      if (parser == null) {
        parser = findParser(pkg + ".extensions." + base + "Parser", cls);
      }
    }
    return parser;
  }

  private static boolean isBounded(String s) {
    return s.startsWith("[") && s.endsWith("]")
        || s.startsWith("\"") && s.endsWith("\"")
        || s.startsWith("'") && s.endsWith("'");
  }

  private static String escapeCommas(String s) {
    return replace(replace(s, ESC_COMMA, ESC_ESC_COMMA), ",", ESC_COMMA);
  }

  private static String unescapeCommas(String s) {
    return replace(replace(s, ESC_COMMA, ","), ESC_ESC_COMMA, ESC_COMMA);
  }

  public static String encodeArguments(String[] args) {
    StringBuilder sb = new StringBuilder();
    if (args.length > 0) {
      if (isBounded(args[0])) {
        sb.append(args[0]);
      } else {
        sb.append(escapeCommas(args[0]));
      }
      for (int i = 1; i < args.length; i++) {
        sb.append(",");
        if (isBounded(args[i])) {
          sb.append(args[i]);
        } else {
          sb.append(escapeCommas(args[i]));
        }
      }
    }
    return sb.toString();
  }

  private static class Tokenizer extends ArrayList<String> {

    public Tokenizer(String input) {
      while (true) {
        int index = input.indexOf(",");
        if (index == -1) {
          add(input);
          break;
        }
        add(input.substring(0, index));
        input = input.substring(index + 1);
      }
    }
  }

  public static String[] parseArgumentList(String encodedArgs) {
    ArrayList<String> alist = new ArrayList<>();
    if (encodedArgs == null || encodedArgs.isEmpty()) {
      return new String[0];
    }
    // handle old method of escaped commas
    encodedArgs = replace(encodedArgs, "\\,", ESC_COMMA);
    Iterator<String> iter = new Tokenizer(encodedArgs).iterator();
    while (iter.hasNext()) {
      StringBuilder str = new StringBuilder(iter.next());

      if (str.toString().trim().startsWith("[") && !str.toString().trim().endsWith("]")) {
        while (iter.hasNext()) {
          String next = iter.next();
          str.append(",").append(next);
          if (next.trim().endsWith("]")) {
            break;
          }
        }
      } else if (str.toString().trim().startsWith("\"") && !str.toString().trim().endsWith("\"")) {
        while (iter.hasNext()) {
          String next = iter.next();
          str.append(",").append(next);
          if (next.trim().endsWith("\"")) {
            break;
          }
        }
      } else if (str.toString().trim().startsWith("'") && !str.toString().trim().endsWith("'")) {
        while (iter.hasNext()) {
          String next = iter.next();
          str.append(",").append(next);
          if (next.trim().endsWith("'")) {
            break;
          }
        }
      }

      if (NULL.equals(str.toString().trim())) {
        alist.add(null);
      } else {
        // If it's an array, don't unescape the commas yet
        if (!str.toString().startsWith("[")) {
          str = new StringBuilder(unescapeCommas(str.toString()));
        }
        alist.add(str.toString());
      }
    }
    return alist.toArray(new String[0]);
  }

  public static String substitute(Resolver resolver, String arg) {
    if (arg == null) {
      return null;
    }

    int i;
    int marker = 0;
    StringBuilder sb = new StringBuilder();
    while ((i = arg.indexOf("${", marker)) != -1) {
      if (marker < i) {
        sb.append(arg, marker, i);
        marker = i;
      }
      int end = arg.indexOf("}", i);
      if (end == -1) {
        break;
      }
      String name = arg.substring(i + 2, end);
      Object value = resolver.getProperty(name);
      if (value == null) {
        value = System.getProperty(name);
      }
      if (value == null) {
        value = arg.substring(i, end + 1);
      }
      sb.append(toString(value));
      marker = end + 1;
    }
    sb.append(arg.substring(marker));
    return sb.toString();
  }

  public static Object eval(Resolver resolver, String arg, Class<?> cls)
      throws IllegalArgumentException, NoSuchReferenceException, ComponentSearchException {
    // Perform property substitution
    arg = substitute(resolver, arg);

    Parser parser;
    Object result;
    try {
      if (arg == null || arg.equals(NULL)) {
        result = null;
      } else if (cls.equals(Boolean.class) || cls.equals(boolean.class)) {
        result = Boolean.valueOf(arg.trim());
      } else if (cls.equals(Short.class) || cls.equals(short.class)) {
        result = Short.valueOf(arg.trim());
      } else if (cls.equals(Integer.class) || cls.equals(int.class)) {
        result = Integer.valueOf(arg.trim());
      } else if (cls.equals(Long.class) || cls.equals(long.class)) {
        result = Long.valueOf(arg.trim());
      } else if (cls.equals(Float.class) || cls.equals(float.class)) {
        result = Float.valueOf(arg.trim());
      } else if (cls.equals(Double.class) || cls.equals(double.class)) {
        result = Double.valueOf(arg.trim());
      } else if (cls.equals(ComponentReference.class)) {
        ComponentReference ref = resolver.getComponentReference(arg.trim());
        if (ref == null) {
          throw new NoSuchReferenceException(
              "The resolver " + resolver + " has no reference '" + arg + "'");
        }
        result = ref;
      } else if (Component.class.isAssignableFrom(cls)) {
        ComponentReference ref = resolver.getComponentReference(arg.trim());
        if (ref == null) {
          throw new NoSuchReferenceException(
              "The resolver " + resolver + " has no reference '" + arg + "'");
        }
        // Avoid requiring the user to wait for a component to become
        // available, in most cases.  In those cases where the
        // component creation is particularly slow, an explicit wait
        // can be added.
        // Note that this is not necessarily a wait for the component
        // to become visible, since menu items are not normally
        // visible even if they're available.
        result = waitForComponentAvailable(ref);
      } else if (cls.equals(String.class)) {
        result = arg;
      } else if (cls.isArray() && arg.trim().startsWith("[")) {
        arg = arg.trim();
        String[] args = parseArgumentList(arg.substring(1, arg.length() - 1));
        Class<?> base = cls.getComponentType();
        Object arr = Array.newInstance(base, args.length);
        for (int i = 0; i < args.length; i++) {
          Object obj = eval(resolver, args[i], base);
          Array.set(arr, i, obj);
        }
        result = arr;
      } else if ((parser = getParser(cls)) != null) {
        result = parser.parse(arg.trim());
      } else {
        String msg =
            Strings.get("parser.conversion_error", new Object[] {arg.trim(), cls.getName()});
        throw new IllegalArgumentException(msg);
      }
      return result;
    } catch (NumberFormatException nfe) {
      String msg = Strings.get("parser.conversion_error", new Object[] {arg.trim(), cls.getName()});
      throw new IllegalArgumentException(msg);
    }
  }

  public static Object[] eval(Resolver resolver, String[] args, Class<?>[] params)
      throws IllegalArgumentException, NoSuchReferenceException, ComponentSearchException {
    Object[] plist = new Object[params.length];
    for (int i = 0; i < plist.length; i++) {
      plist[i] = eval(resolver, args[i], params[i]);
    }
    return plist;
  }

  public static String replace(String str, String s1, String s2) {
    StringBuilder sb = new StringBuilder(str);
    int index = 0;
    while ((index = sb.toString().indexOf(s1, index)) != -1) {
      sb.delete(index, index + s1.length());
      sb.insert(index, s2);
      index += s2.length();
    }
    return sb.toString();
  }

  // TODO: move this somewhere more appropriate; make public static, maybe in ComponentReference
  private static Component waitForComponentAvailable(ComponentReference ref)
      throws ComponentSearchException {
    try {
      ComponentTester tester = ComponentTester.getTester(Component.class);
      tester.wait(
          new Condition() {
            @Override
            public boolean test() {
              try {
                ref.getComponent();
              } catch (ComponentNotFoundException e) {
                return false;
              } catch (MultipleComponentsFoundException m) {
                // ignore
              }
              return true;
            }

            @Override
            public String toString() {
              return ref + " to become available";
            }
          },
          ComponentTester.componentDelay);
    } catch (WaitTimedOutError wto) {
      String msg = "Could not find " + ref + ": " + Step.toXMLString(ref);
      throw new ComponentNotFoundException(msg);
    }
    return ref.getComponent();
  }

  public static String toString(Object value) {
    if (value == null) {
      return NULL;
    }
    if (value.getClass().isArray()) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (int i = 0; i < Array.getLength(value); i++) {
        Object o = Array.get(value, i);
        if (i > 0) {
          sb.append(",");
        }
        sb.append(toString(o));
      }
      sb.append("]");
      return sb.toString();
    }
    String s = value.toString();
    if (s == null) {
      return NULL;
    }

    if (isDefaultToString(s)) {
      return DEFAULT_TOSTRING;
    }
    return s;
  }

  public static boolean isDefaultToString(String s) {
    if (s == null) {
      return false;
    }

    int at = s.indexOf("@");
    if (at != -1) {
      String hash = s.substring(at + 1);
      try {
        Integer.parseInt(hash, 16);
        return true;
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    return false;
  }
}
