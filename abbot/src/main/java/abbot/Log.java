package abbot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Various logging, assertion, and debug routines.  Typical usage is to include the following code
 * <pre>
 * public static void main(String[] args) {
 * &nbsp; args = Log.init(args)
 * &nbsp; ...
 * }
 * </pre>
 * at an application's main entry point.  This way the Log class can remove its options from the
 * full set passed into the application.  See the {@link #init(String[]) Log.init} method for
 * initialization options.
 * General usage notes on public functions:
 *
 * <ul>
 * <li><b>warn</b><br>
 * Programmer warnings; things that you think shouldn't be happening or
 * indicate something might be wrong.  Warnings typically mean "Something
 * happened that I didn't expect would happen".
 * <li><b>log</b><br>
 * Important information that might be needed for later reference; things the
 * user or debugger might be interested in.  By default, all messages go
 * here.  Logs are made available so that the customer may provide us with an
 * accurate record of software activity.<br>
 * All warnings and failed assertions are written to the log.  Debug
 * statements are also written to log in non-release code.
 * <li><b>debug</b><br>
 * Any messages which might be useful for debugging (non-release code
 * only).
 * <li><b>assertTrue</b><br>
 * Assumed preconditions for proper execution, also referred to as
 * invariants.
 * </ul>
 * Per-class stack trace depth can be specified when adding a class, e.g.
 * classname[:stack-depth].
 *
 * @author twall
 * @version $Revision: 1.1 $
 */
public final class Log {

  /**
   * No instantiations.
   */
  private Log() {}

  /**
   * Global final to determine whether debugging code is generated. This should be changed too false
   * to build production code.
   */
  private static final boolean DEBUG_BUILD = true;

  /**
   * Mnemonic to print all lines of a stack trace.
   */
  public static final int FULL_STACK = 0;

  /**
   * Mnemonic to print the default lines of a stack trace.
   */
  private static final int CLASS_STACK_DEPTH = -1;

  /**
   * Basic warning categories.
   * FIXME: use these:
   * public static final int ERROR   = 0x0001;
   * public static final int WARNING = 0x0002;
   * public static final int DEBUG   = 0x0004;
   * public static final int INFO    = 0x0008;
   */
  private static class LogSynchronizer {}

  /**
   * Synchronize message output.
   */
  private static final LogSynchronizer synchronizer = new LogSynchronizer();

  /**
   * Whether any debugging output is enabled.
   */
  public static boolean expectDebugOutput;

  /**
   * Whether to log messages. Default on so that we capture output until the log file has been set
   * or not set in {@link #init(String[])}.
   */
  private static boolean logMessages = true;

  /**
   * Whether to print programmer warnings.
   */
  private static boolean printConsoleWarnings = DEBUG_BUILD;

  /**
   * Whether to show threads in debug output.
   */
  private static boolean showThreads;

  /**
   * Default number of lines of stack trace to print.
   */
  private static int debugStackDepth = 1;

  /**
   * Default number of lines of exception stack trace to print.
   */
  private static int excStackDepth = FULL_STACK;

  /**
   * Show timestamps in the log?
   */
  private static boolean showTimestamp = true;

  /**
   * Store which classes we want to see debug info for.
   * FIXME: make it a map and make the value the debug level.
   */
  private static final HashMap<Class<?>, Integer> debugged = new HashMap<>();

  /**
   * Store which classes we don't want to see debug info for
   */
  private static final HashSet<Class<?>> notdebugged = new HashSet<>();

  /**
   * Debug all classes?
   */
  private static boolean debugAll;

  /**
   * Treat inner/anonymous classes as outer class?
   */
  private static final boolean DEBUG_INNER = true;

  private static final String DEFAULT_LOGFILE_NAME = "abbot.log";
  private static final ByteArrayOutputStream preInitLog = new ByteArrayOutputStream();
  private static PrintStream log = new PrintStream(preInitLog);

  // Save these for future use
  private static final PrintStream SYSTEM_OUT = System.out;
  private static final PrintStream SYSTEM_ERR = System.err;

  /**
   * Debug/log initialization, presumably from the command line.
   * <br>Recognized options:
   * <pre>
   * --debug all | className[:depth] | *.partialClassName[:depth]
   * --no-debug className | *.partialClassName
   * --log log file name
   * --no-timestamp
   * --enable-warnings
   * --show-threads
   * --stack-depth depth
   * --exception-depth depth
   * </pre>
   * @param args command line arguments
   * @return result
   */
  public static String[] init(String[] args) {

    if (!DEBUG_BUILD) {
      // Redirect System.err & System.out
      PrintStream nullStream =
          new PrintStream(
              new OutputStream() {
                @Override
                public void write(int b) {}
              });
      System.setErr(nullStream);
      System.setOut(nullStream);
    }

    List<String> newArgs = new ArrayList<>(args.length);
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--enable-warnings" -> printConsoleWarnings = true;
        case "--no-timestamp" -> showTimestamp = false;
        case "--show-threads" -> showThreads = true;
        case "--stack-depth" -> {
          if (++i < args.length) {
            try {
              debugStackDepth = Integer.parseInt(args[i]);
            } catch (Exception e) {
              // ignore
            }
          } else {
            internalWarn("Ignoring --stack-depth with no argument");
          }
        }
        case "--exception-depth" -> {
          if (++i < args.length) {
            try {
              excStackDepth = Integer.parseInt(args[i]);
            } catch (Exception e) {
              // ignore
            }
          } else {
            internalWarn("Ignoring --exception-depth with no argument");
          }
        }
        case "--debug", "--no-debug" -> {
          // since we're enabling some debugging, set the other settings
          // to debug defaults...
          boolean exclude = args[i].startsWith("--no");

          // Re-enable stdout/stderr if they were removed
          if (!DEBUG_BUILD) {
            System.setOut(SYSTEM_OUT);
            System.setErr(SYSTEM_ERR);
          }
          if (++i < args.length) {
            if (exclude) {
              removeDebugClass(args[i]);
            } else {
              addDebugClass(args[i]);
            }
          } else {
            internalWarn("Ignoring " + args[i - 1] + " with no argument");
          }
        }
        case "--log" -> {
          String filename = DEFAULT_LOGFILE_NAME;
          if (++i < args.length) {
            filename = args[i];
          }
          enableLogging(filename);
        }
        default -> newArgs.add(args[i]);
      }
    }
    String[] result = new String[newArgs.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = newArgs.get(i);
    }
    return result;
  }

  public static boolean loggingEnabled() {
    return logMessages && log != null;
  }

  /**
   * Enable log output to the given file.  If the filename given is "-", stdout is used instead of a
   * file.
   * @param filename file name
   */
  public static void enableLogging(String filename) {
    logMessages = true;
    try {
      if (filename.equals("-")) {
        log = SYSTEM_OUT;
      } else {
        log = new PrintStream(new FileOutputStream(filename), true);
      }
      String hostname = "127.0.0.1";
      try {
        hostname = java.net.InetAddress.getLocalHost().getHostName();
      } catch (java.net.UnknownHostException uhe) {
        internalWarn("Can't get hostname, using " + hostname);
      }
      log("Log started on " + hostname);
      if (!DEBUG_BUILD) {
        // Prefer output to go to the log on a release build, since
        // there may not be any console.
        if (expectDebugOutput) {
          SYSTEM_OUT.println("Output redirected.  See the log " + "file for debug output.");
        }
        System.setErr(log);
        System.setOut(log);
      }
      // Make sure the log gets closed on System.exit
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread("Log shutdown hook") {
                public void run() {
                  close();
                }
              });
    } catch (FileNotFoundException exc) {
      internalWarn("Can't open log file " + filename);
    }
  }

  public static void removeDebugClass(String id) {
    setDebugClass(id, false);
  }

  public static void removeDebugClass(Class<?> c) {
    notdebugged.add(c);
    debugged.remove(c);
    Log.debug("Debugging disabled for " + c);
    expectDebugOutput = !debugged.isEmpty() || debugAll;
  }

  public static void addDebugClass(String id) {
    setDebugClass(id, true);
  }

  public static void addDebugClass(Class<?> c) {
    addDebugClass(c, CLASS_STACK_DEPTH);
  }

  public static void addDebugClass(Class<?> c, int depth) {
    expectDebugOutput = true;
    debugged.put(c, depth);
    notdebugged.remove(c);
    Log.debug("Debugging enabled for " + c);
  }

  private static void setDebugClass(String id, boolean enable) {
    if (id.equals("all")) {
      debugAll = enable;
      if (enable) {
        notdebugged.clear();
        expectDebugOutput = true;
      } else {
        debugged.clear();
        expectDebugOutput = false;
      }
    } else {
      int colon = id.indexOf(":");
      String className = colon == -1 ? id : id.substring(0, colon);
      try {
        Class<?> c = getClassFromDescriptor(className);
        try {
          int depth = colon == -1 ? debugStackDepth : Integer.parseInt(id.substring(colon + 1));
          if (enable) {
            addDebugClass(c, depth);
          } else {
            removeDebugClass(c);
          }
        } catch (NumberFormatException nfe) {
          if (enable) {
            addDebugClass(c);
          } else {
            removeDebugClass(c);
          }
        }
      } catch (ClassNotFoundException exc) {
        internalWarn("Class '" + className + "' not found");
      }
    }
  }

  /**
   * Returns class from given name/descriptor.  Descriptor can be either a fully qualified
   * classname, or a classname beginning with *. with client-specific package and classname
   * following.
   * @param className class name
   * @return class
   * @throws ClassNotFoundException if class was not found
   */
  private static Class<?> getClassFromDescriptor(String className) throws ClassNotFoundException {
    return Class.forName(className);
  }

  private static String getStackTrace(int pop, int lines) {
    return getStackTrace(pop, lines, new Throwable("--debug--"));
  }

  private static String getStackTrace(int pop, int lines, Throwable thr) {
    String stack = getStackTrace(pop, thr);
    if (lines == FULL_STACK) {
      return stack;
    }
    return trimStackTrace(stack, lines);
  }

  private static String getStackTrace(int pop, Throwable thr) {
    OutputStream os = new ByteArrayOutputStream();
    PrintStream newStream = new PrintStream(os, true);
    // OUCH! this is a serious performance hit!
    thr.printStackTrace(newStream);
    String trace = os.toString();

    // Pop off getStackTrace itself
    // Skip over any calls to getStackTrace; the JIT sometimes puts a
    // spurious entry, so don't just stop at the first one.
    int getLoc = trace.lastIndexOf("getStackTrace");
    trace = trace.substring(trace.indexOf("\tat ", getLoc) + 3);
    while (pop-- > 0) {
      // pop off the calling function
      int at = trace.indexOf("\tat ");
      if (at != -1) {
        trace = trace.substring(at + 3);
      }
    }

    return trace;
  }

  private static String trimStackTrace(String trace, int lines) {
    int end = trace.indexOf(")") + 1;
    boolean all = (lines == FULL_STACK);
    while (all || --lines > 0) {
      int index = trace.indexOf("\tat ", end);
      if (index < 0) {
        break;
      }
      end = trace.indexOf(")", index) + 1;
    }
    return trace.substring(0, end);
  }

  // FIXME with JIT enabled, stack trace sometimes has spurious junk on the
  // stack, which will indicate the wrong class...
  private static Class<?> extractClass(String trace) {
    String tmp = trace.substring(0, trace.indexOf("("));
    String cname = tmp.substring(0, tmp.lastIndexOf("."));
    if (DEBUG_INNER) {
      int sub = cname.indexOf("$");
      if (sub >= 0) {
        cname = cname.substring(0, sub);
      }
    }
    try {
      cname = cname.trim();
      return Class.forName(cname);
    } catch (ClassNotFoundException exc) {
      internalWarn("Class '" + cname + "' not found");
      return null;
    }
  }

  public static boolean isClassDebugEnabled(Class<?> c) {
    return (debugAll || debugged.containsKey(c)) && !notdebugged.contains(c);
  }

  private static int getClassStackDepth(Class<?> c) {
    Integer depth = debugged.get(c);
    return depth != null && depth != CLASS_STACK_DEPTH ? depth : debugStackDepth;
  }

  private static void internalDebug(Class<?> cls, String msg, int lines) {
    if (cls == null || isClassDebugEnabled(cls)) {
      String stack = getStackTrace(2, FULL_STACK);
      if (cls != null || isClassDebugEnabled(cls = extractClass(stack))) {
        String threadName = showThreads ? ": [" + Thread.currentThread().getName() + "] " : ": ";
        if (lines == CLASS_STACK_DEPTH) {
          lines = getClassStackDepth(cls);
        }
        internalWarn(trimStackTrace(stack, lines) + threadName + msg);
      }
    }
  }

  public static void debug(Class<?> c, String event) {
    if (expectDebugOutput) {
      internalDebug(c, event, CLASS_STACK_DEPTH);
    }
  }

  public static void debug(String event) {
    if (expectDebugOutput) {
      internalDebug(null, event, CLASS_STACK_DEPTH);
    }
  }

  public static void debug(String event, int lines) {
    if (expectDebugOutput) {
      internalDebug(null, event, lines);
    }
  }

  public static void debug() {
    if (expectDebugOutput) {
      internalDebug(null, "", CLASS_STACK_DEPTH);
    }
  }

  public static void debug(Throwable thr) {
    if (expectDebugOutput) {
      String where = getStackTrace(0, excStackDepth, thr);
      String here = getStackTrace(1, debugStackDepth);
      String type = thr instanceof Error ? "Error" : "Exception thrown";
      internalDebug(null, type + " at " + where + ": " + thr + " (caught at " + here + ")", 1);
      if (thr instanceof InvocationTargetException exception) {
        thr = exception.getTargetException();
        where = getStackTrace(0, excStackDepth, thr);
        internalDebug(null, "Target exception was " + thr + " at " + where, 1);
      }
    }
  }

  public static void debug(Object obj) {
    if (expectDebugOutput) {
      internalDebug(null, obj == null ? "(null)" : obj.toString(), CLASS_STACK_DEPTH);
    }
  }

  private static String abbreviate(String msg) {
    return msg;
  }

  private static void internalWarn(String message) {
    synchronized (synchronizer) {
      internalLog(message);
      if (printConsoleWarnings && log != SYSTEM_ERR && log != SYSTEM_OUT) {
        System.err.println(abbreviate(message));
      }
    }
  }

  public static String getStack(int lines) {
    return getStackTrace(1, lines);
  }

  public static String getStack(int lines, Throwable thr) {
    return getStackTrace(1, lines, thr);
  }

  public static void warn(String message) {
    warn(message, debugStackDepth);
  }

  public static void warn(String message, int lines) {
    String stack = getStackTrace(1, lines);
    internalWarn(stack + ": " + message);
  }

  public static void warn(Throwable thr) {
    String where = getStackTrace(0, excStackDepth, thr);
    String here = getStackTrace(1, debugStackDepth);
    String type = thr instanceof Error ? "Error" : "Exception thrown";
    internalWarn(type + " at " + where + ": " + thr + " (caught at " + here + ")");
    if (thr instanceof InvocationTargetException exception) {
      thr = exception.getTargetException();
      where = getStackTrace(0, excStackDepth, thr);
      internalWarn("Target exception was " + thr + " at " + where);
    }
  }

  public static void log(Throwable thr) {
    if (loggingEnabled()) {
      String stack = getStackTrace(0, excStackDepth, thr);
      internalLog("Exception thrown: " + thr + "->" + stack);
    }
  }

  public static void log(String message, Throwable thr) {
    if (loggingEnabled()) {
      String stack = getStackTrace(0, excStackDepth, thr);
      internalLog("Exception thrown: " + message + ": " + thr + "->" + stack);
    }
  }

  public static void log(String message) {
    if (loggingEnabled()) {
      if (debugStackDepth != 1) {
        String stack = getStackTrace(1, 1);
        internalLog(stack + ": " + message);
      } else {
        internalLog(message);
      }
    }
  }

  private static void internalLog(String event) {
    if (loggingEnabled()) {
      StringBuilder msg = new StringBuilder();
      if (showTimestamp) {
        var dateFormat = new SimpleDateFormat("yyMMdd HH:mm:ss:SSS ");
        msg.append(dateFormat.format(new Date()));
      }
      msg.append(abbreviate(event));
      synchronized (synchronizer) {
        log.println(msg);
      }
    }
  }

  public static void close() {
    if (loggingEnabled()) {
      log("Log closed");
      try {
        log.close();
      } catch (Exception e) {
        // ignore
      }
    }
  }
}
