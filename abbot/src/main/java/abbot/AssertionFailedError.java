package abbot;

import abbot.i18n.Strings;
import abbot.script.Script;
import abbot.script.Step;
import java.io.File;

/**
 * Indirect usage to avoid too much direct linkage to JUnit.
 */
public class AssertionFailedError extends RuntimeException {

  public AssertionFailedError() {
    this("");
  }

  public AssertionFailedError(String msg) {
    super(msg);
  }

  public AssertionFailedError(String msg, Step step) {
    super(getMessage(msg, step));
  }

  private static String getMessage(String msg, Step step) {
    int line = Script.getLine(step);
    if (line <= 0) {
      return msg;
    }

    File file = Script.getFile(step);
    return Strings.get("step.failure", new Object[] {msg, file, line});
  }
}
