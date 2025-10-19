package abbot.util;

import abbot.Log;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.PrintStream;
import java.util.Properties;
import javax.swing.UIManager;

/**
 * Preserve and restore system state. This includes the following:
 * <ul>
 * <li><code>System.out/err</code> streams
 * <li><code>System</code> properties
 * <li>Security manager
 * </ul>
 */
public class SystemState {

  private static final int[] CODES = {
    KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_NUM_LOCK, KeyEvent.VK_SCROLL_LOCK, KeyEvent.VK_KANA_LOCK
  };
  private final Properties oldProps;
  private final PrintStream oldOut;
  private final PrintStream oldErr;
  private final String oldLookAndFeel;
  private final boolean[] lockingKeys;
  private static Robot robot = null;

  static {
    try {
      robot = new Robot();
    } catch (AWTException e) {
      // ignore
    }
  }

  /**
   * Take a snapshot of the current System state for later restoration.
   */
  public SystemState() {
    lockingKeys = new boolean[CODES.length];
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    for (int i = 0; i < CODES.length; i++) {
      try {
        lockingKeys[i] = toolkit.getLockingKeyState(CODES[i]);
        try {
          toolkit.setLockingKeyState(CODES[i], false);
        } catch (UnsupportedOperationException e) {
          // Manually toggle the key
          if (lockingKeys[i]) {
            toggleKey(i);
          }
        }
      } catch (UnsupportedOperationException e) {
        // Nothing much we can do
      }
    }
    oldLookAndFeel = UIManager.getLookAndFeel().getClass().getName();
    oldOut = System.out;
    oldErr = System.err;
    System.setOut(new ProtectedStream(oldOut));
    System.setErr(new ProtectedStream(oldErr));
    oldProps = (Properties) System.getProperties().clone();
  }

  /**
   * Restore the state captured in the ctor.
   */
  public void restore() {
    System.setProperties(oldProps);
    System.setOut(oldOut);
    System.setErr(oldErr);
    try {
      UIManager.setLookAndFeel(oldLookAndFeel);
    } catch (Exception e) {
      Log.warn("Could not restore LAF: " + e);
    }
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    for (int i = 0; i < CODES.length; i++) {
      try {
        boolean state = toolkit.getLockingKeyState(CODES[i]);
        if (state != lockingKeys[i]) {
          try {
            toolkit.setLockingKeyState(CODES[i], lockingKeys[i]);
          } catch (UnsupportedOperationException e) {
            toggleKey(i);
          }
        }
      } catch (UnsupportedOperationException e) {
        // ignore
      }
    }
  }

  private void toggleKey(int i) {
    if (robot != null) {
      robot.keyPress(CODES[i]);
      robot.keyRelease(CODES[i]);
    }
  }

  /**
   * Provide a wrapper that prevents the original stream from being closed.
   */
  private static class ProtectedStream extends PrintStream {

    private boolean closed = false;

    public ProtectedStream(PrintStream original) {
      super(original);
    }

    @Override
    public void flush() {
      if (!closed) {
        super.flush();
      }
    }

    @Override
    public void close() {
      closed = true;
    }

    @Override
    public void write(int b) {
      if (!closed) {
        super.write(b);
      }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
      if (!closed) {
        super.write(buf, off, len);
      }
    }
  }
}
