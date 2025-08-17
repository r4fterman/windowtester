package abbot.script;

import org.dom4j.Element;

/**
 * Exception to indicate the script being parsed is invalid.
 */
public class InvalidScriptException extends RuntimeException {
  public InvalidScriptException(String msg) {
    super(msg);
  }

  public InvalidScriptException(String msg, Element el) {
    super(msg + " (when parsing " + el.toString() + ")");
  }
}
