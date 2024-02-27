package junit.extensions.abbot;

import abbot.Log;
import abbot.script.Script;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Similar to TestSuite, except that it auto-generates a suite based on test scripts matching
 * certain criteria.
 * <p>
 * By default, generate a suite of all scripts found in a given directory for which the accept
 * method returns true. Note that there is no guarantee of the order of the scripts.
 * <p>
 * The ScriptTestSuite constructors which require a class argument provide a means for using custom
 * fixtures derived from
 * <a href="ScriptFixture.html">ScriptFixture</a>.  The default fixture
 * preserves existing environment windows (e.g. the JUnit Swing UI TestRunner) and disposes of all
 * windows generated by the code under test.  Derived fixtures may provide arbitrary code in their
 * setUp/tearDown methods (such as install/uninstall a custom security manager, set system
 * properties, etc), the same as you would do in  any other derivation of junit.framework.TestCase.
 *
 * <h3>Example 1</h3>
 * Following is a ScriptTestSuite which will aggregate all tests in the directory "src/example",
 * whose filenames begin with "MyCode-" and end with ".xml":<br>
 * <pre><code>
 * public class MyCodeTest extends ScriptFixture {
 *     public MyCodeTest(String name) { super(name); }
 *     public static Test suite() {
 *         return new ScriptTestSuite(MyCodeTest.class, "src/example") {
 *             public boolean accept(File file) {
 *                 String name = file.getName();
 *                 return name.startsWith("MyCode-") &amp;&amp; name.endsWith(".xml");
 *             }
 *         };
 *     }
 * }
 * </code></pre>
 */
public class ScriptTestSuite extends TestSuite {

  private File primaryDirectory;

  /**
   * Constructs a suite of tests from all the scripts found in the directory specified by the system
   * property "abbot.testsuite.path". The most common use for this constructor would be from an Ant
   * 'junit' task, where the system property is defined for a given run. The suite will recurse
   * directories if "abbot.testsuite.path.recurse" is set to true.
   */
  public ScriptTestSuite() {
    this(
        ScriptFixture.class,
        System.getProperty("abbot.testsuite.path", System.getProperty("user.dir")),
        Boolean.getBoolean("abbot.testsuite.path.recurse"));
  }

  /**
   * Constructs a suite of tests from all the scripts found in the current directory.  Does not
   * recurse to subdirectories.  The Class argument must be a subclass of
   * junit.extensions.abbot.ScriptFixture.
   */
  public ScriptTestSuite(final Class<?> fixtureClass) {
    this(fixtureClass, System.getProperty("user.dir"), false);
  }

  /**
   * Constructs a suite of tests from all the scripts found in the given directory.  Does not
   * recurse to subdirectories. The Class argument must be a subclass of
   * junit.extensions.abbot.ScriptFixture.
   */
  public ScriptTestSuite(final Class<?> fixtureClass, final String dirname) {
    this(fixtureClass, dirname, false);
  }

  /**
   * Constructs an ScriptTestSuite from all the scripts in the given directory, recursing if recurse
   * is true. The Class argument must be a class derived from junit.extensions.abbot.ScriptFixture.
   */
  public ScriptTestSuite(final Class<?> fixtureClass, final String dirname, final boolean recurse) {
    this(fixtureClass, findFilenames(dirname, recurse));
    primaryDirectory = new File(dirname);
    if (!primaryDirectory.exists() || !primaryDirectory.isDirectory()) {
      final String msg =
          "Directory '" + dirname + "' did not exist" + " when scanning for test scripts";
      addTest(warnings(msg));
    }
  }

  /**
   * Constructs a suite of tests for each script given in the argument list.
   */
  public ScriptTestSuite(final String[] filenames) {
    this(ScriptFixture.class, filenames);
  }

  /**
   * Constructs a suite of tests for each script given in the argument list, using the given class
   * derived from ScriptFixture to wrap each script.
   */
  public ScriptTestSuite(final Class<?> fixtureClass, final String[] filenames) {
    super(fixtureClass.getName());
    primaryDirectory = new File(System.getProperty("user.dir"));
    String msg = "Loading " + fixtureClass + ", with " + filenames.length + " files";
    Log.debug(msg);
    System.err.println(msg);
    for (String filename : filenames) {
      final File file = new File(filename);
      // Filter for desired files only
      if (!accept(file)) {
        continue;
      }
      try {
        Log.debug("Attempting to create " + fixtureClass);
        final Constructor<?> ctor = fixtureClass.getConstructor(String.class);
        final Test test = (Test) ctor.newInstance(file.getAbsolutePath());
        Log.debug("Created an instance of " + fixtureClass);
        addTest(test);
      } catch (final Throwable thr) {
        Log.warn(thr);
        addTest(warnings("Could not construct an instance of " + fixtureClass));
        break;
      }
    }
    if (testCount() == 0) {
      addTest(warnings("No scripts found"));
    }
  }

  public File getDirectory() {
    return primaryDirectory;
  }

  /**
   * Return whether to accept the given file.   The default implementation omits common backup
   * files.
   */
  public boolean accept(final File file) {
    final String name = file.getName();
    return !name.startsWith(".#") && !name.endsWith("~") && !name.endsWith(".bak");
  }

  /**
   * Returns a test which will fail and log a warning message.
   */
  private Test warnings(final String message) {
    return new TestCase("warning") {
      @Override
      protected void runTest() {
        fail(message);
      }
    };
  }

  /**
   * Add all test scripts in the given directory, optionally recursing to subdirectories.  Returns a
   * list of absolute paths.
   */
  protected static List<String> findTestScripts(
      final File dir, final List<String> files, final boolean recurse) {
    final File[] flist = dir.listFiles();
    for (int i = 0; flist != null && i < flist.length; i++) {
      // Log.debug("Examining " + flist[i]);
      if (flist[i].isDirectory()) {
        if (recurse) {
          findTestScripts(flist[i], files, recurse);
        }
      } else if (Script.isScript(flist[i])) {
        final String filename = flist[i].getAbsolutePath();
        if (!files.contains(filename)) {
          Log.debug("Adding " + filename);
          files.add(filename);
        }
      }
    }
    return files;
  }

  /**
   * Scan for test scripts and return an array of filenames for all scripts found.
   */
  static String[] findFilenames(final String dirname, final boolean recurse) {
    final File dir = new File(dirname);
    final List<String> list = new ArrayList<>();
    if (dir.exists() && dir.isDirectory()) {
      findTestScripts(dir, list, recurse);
    }
    return list.toArray(new String[0]);
  }
}
