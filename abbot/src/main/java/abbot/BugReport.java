package abbot;

import abbot.i18n.Strings;
import abbot.tester.Robot;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.UIManager;

/**
 * Exception for reporting unexpected situations in the program. Automatically generates a message suitable for posting
 * in a bug report.
 */
public class BugReport extends Error implements Version {

  private static final String LINE_SEPARATOR = System.lineSeparator();
  private static final String BUG_REPORT_URL = Strings.get("bugreport.url");

  private static String getReportingInfo() {
    return Strings.get(
        "bugreport.info", new Object[] {LINE_SEPARATOR + BUG_REPORT_URL + LINE_SEPARATOR});
  }

  public static String getSystemInfo() {
    return "mode: "
        + Robot.getEventModeDescription()
        + LINE_SEPARATOR
        + "           OS: "
        + System.getProperty("os.name")
        + " "
        + System.getProperty("os.version")
        + " ("
        + System.getProperty("os.arch")
        + ") "
        + LINE_SEPARATOR
        + " Java version: "
        + System.getProperty("java.version")
        + " (vm "
        + System.getProperty("java.vm.version")
        + ")"
        + LINE_SEPARATOR
        + "    Classpath: "
        + System.getProperty("java.class.path")
        + LINE_SEPARATOR
        + "Look and Feel: "
        + UIManager.getLookAndFeel();
  }

  private final String errorMessage;
  private final Throwable throwable;

  public BugReport(String error) {
    this(error, null);
  }

  public BugReport(String error, Throwable thr) {
    super(error);
    errorMessage = error;
    throwable = thr;
  }

  @Override
  public String toString() {
    String exc = "";
    if (throwable != null) {
      StringWriter writer = new StringWriter();
      throwable.printStackTrace(new PrintWriter(writer));
      exc = writer.toString();
    }
    return errorMessage
        + LINE_SEPARATOR
        + getReportingInfo()
        + LINE_SEPARATOR
        + getSystemInfo()
        + LINE_SEPARATOR
        + exc;
  }
}
