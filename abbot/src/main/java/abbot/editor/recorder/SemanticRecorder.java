package abbot.editor.recorder;

import abbot.BugReport;
import abbot.script.Resolver;
import abbot.script.Step;
import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Template for recording AWTEvents and converting them into an appropriate semantic event.  The EventRecorder class
 * decides which SemanticRecorder to use and handles cancel/termination.  Implementations should be named
 * AbstractButtonRecorder, JTableRecorder, etc.  The semantic recorders will be dynamically loaded based on the
 * component receiving a given event (compare to ComponentTester).
 *
 * See EventRecorder for implementation details.
 */
public abstract class SemanticRecorder implements SemanticEvents {
  private ActionListener al;
  private final Resolver resolver;
  protected ArrayList events = new ArrayList();

  private int recordingType = SE_NONE;
  private volatile boolean isFinished;
  private Step step = null;
  private BugReport bug;

  /**
   * Create a SemanticRecorder for use in capturing the semantics of a GUI action.
   * @param resolver resolver
   */
  public SemanticRecorder(Resolver resolver) {
    this.resolver = resolver;
  }

  /**
   * Supports at most one listener.
   * @param al action listener
   */
  public void addActionListener(ActionListener al) {
    this.al = al;
  }

  public int getRecordingType() {
    return recordingType;
  }

  protected void setRecordingType(int type) {
    recordingType = type;
  }

  protected void init(int recordingType) {
    events.clear();
    step = null;
    bug = null;
    setFinished(false);
    setRecordingType(recordingType);
  }

  public abstract boolean accept(AWTEvent event);

  public final boolean record(java.awt.AWTEvent event) {
    if (!isFinished()) {
      try {
        if (parse(event)) {
          // Maintain a list of all events parsed for future
          // reference
          events.add(event);
          return true;
        }
      } catch (BugReport br) {
        setFinished(true);
        bug = br;
      }
    }
    return false;
  }

  /**
   * Handle an event.  Return true if the event has been consumed. Returning false usually means that isFinished()
   * will return true.
   * @param event event
   * @return true if event has been consumed
   */
  public abstract boolean parse(AWTEvent event);

  /**
   * Return the Resolver to be used by this recorder.
   * @return resolver
   */
  protected Resolver getResolver() {
    return resolver;
  }

  /**
   * Returns the script step generated from the events recorded so far.  If no real action resulted, may return null
   * (for example, a mouse press on a button which releases outside the button).
   * @return step
   */
  public synchronized Step getStep() throws BugReport {
    if (bug != null) {
      throw bug;
    }

    if (step == null) {
      step = createStep();
    }
    return step;
  }

  /**
   * Create a step based on the events received thus far.  Returns null if no semantic event or an imcomplete event
   * has been detected.
   * @return step
   */
  protected abstract Step createStep();

  /**
   * Add the given step.  Should be used when the recorder detects a complete semantic event sequence.  After this
   * point, the recorder will no longer accept events.
   * @param newStep new step
   */
  protected synchronized void setStep(Step newStep) {
    events.clear();
    step = newStep;
  }

  /**
   * Return whether this recorder has finished recording the current semantic event.
   * @return true if finished
   */
  public synchronized boolean isFinished() {
    return isFinished;
  }

  /**
   * Invoke when end of the semantic event has been seen.
   * @param state state
   */
  protected synchronized void setFinished(boolean state) {
    isFinished = state;
  }

  /**
   * Indicate the current recording state, so that the status may be displayed elsewhere.
   * @param msg message
   */
  protected void setStatus(String msg) {
    if (al != null) {
      ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, msg);
      al.actionPerformed(event);
    }
  }
}
