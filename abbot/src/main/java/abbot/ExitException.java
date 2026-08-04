package abbot;

/**
 * Provide a tagging interface and storage for attempted exits from code under test.
 */
public class ExitException extends SecurityException {

  public ExitException(String msg, int status) {
    super(msg + " (" + status + ") on " + Thread.currentThread());
    Log.log("Exit exception created at " + Log.getStack(Log.FULL_STACK, this));
  }
}
