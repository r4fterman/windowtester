package abbot.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A task that can be scheduled for one-time or repeated execution by a {@link Timer}.
 *
 * <p>A timer task is <em>not</em> reusable.  Once a task has been scheduled
 * for execution on a {@code Timer} or cancelled, subsequent attempts to schedule it for execution
 * will throw {@code IllegalStateException}.
 *
 * @author Josh Bloch
 * @since 1.3
 */
public abstract class AbbotTimerTask extends TimerTask {

  /**
   * This task has not yet been scheduled.
   */
  private static final int VIRGIN = 0;

  /**
   * This task is scheduled for execution.  If it is a non-repeating task, it has not yet been
   * executed.
   */
  private static final int SCHEDULED = 1;

  /**
   * This task has been cancelled (with a call to AbbotTimerTask.cancel).
   */
  private static final int CANCELLED = 3;

  /**
   * This object is used to control access to the AbbotTimerTask internals.
   */
  private final Object lock = new Object();

  /**
   * The state of this task, chosen from the constants below.
   */
  private int state = VIRGIN;

  /**
   * Creates a new timer task.
   */
  protected AbbotTimerTask() {}

  /**
   * The action to be performed by this timer task.
   */
  @Override
  public abstract void run();

  @Override
  public boolean cancel() {
    synchronized (lock) {
      boolean result = (state == SCHEDULED);
      state = CANCELLED;
      return result;
    }
  }

  public boolean isCanceled() {
    return state == CANCELLED;
  }
}
