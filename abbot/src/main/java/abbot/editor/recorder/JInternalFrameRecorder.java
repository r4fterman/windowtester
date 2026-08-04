package abbot.editor.recorder;

import abbot.Log;
import abbot.script.Action;
import abbot.script.Assert;
import abbot.script.ComponentReference;
import abbot.script.Resolver;
import abbot.script.Step;
import abbot.tester.ComponentTester;
import abbot.util.AWT;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;

/**
 * Handle the recording of events related to an internal frame (JInternalFrame). Like instances of Window, we must
 * insert waits for the showing and hiding of internal frames.
 *
 * NOTE: InternalFrameEvents are not normally posted to the AWT event queue.
 *
 * @author pickens
 * @author twall
 */
public class JInternalFrameRecorder extends JComponentRecorder {
  private JInternalFrame frame;
  private String type;
  private static final String UNKNOWN = "unknown";
  private static final String SHOW = "show";
  private static final String HIDE = "hide";
  private static final String CLOSE = "close";
  private static final String ICONIFY = "iconify";
  private static final String DEICONIFY = "deiconify";
  private static final String MOVE = "move";
  private static final String RESIZE = "resize";

  public static final int SE_INTERNAL_FRAME = SE_ACTION_MAP + 1;
  public static final int SE_DECORATION = SE_INTERNAL_FRAME + 1;

  /**
   * Constructor for JInternalFrameRecorder.
   *
   * @param resolver resolver
   */
  public JInternalFrameRecorder(Resolver resolver) {
    super(resolver);
  }

  @Override
  protected void init(int rtype) {
    super.init(rtype);
    frame = null;
    type = UNKNOWN;
  }

  @Override
  public boolean accept(AWTEvent event) {
    int id = event.getID();
    Log.debug("Source is " + event.getSource());
    if (event.getSource() instanceof JInternalFrame) {
      if (id == ComponentEvent.COMPONENT_SHOWN
          || id == ComponentEvent.COMPONENT_HIDDEN
          || id == ComponentEvent.COMPONENT_MOVED
          || id == ComponentEvent.COMPONENT_RESIZED
          || id == InternalFrameEvent.INTERNAL_FRAME_CLOSING
          || id == InternalFrameEvent.INTERNAL_FRAME_ICONIFIED
          || id == InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED) {
        init(SE_INTERNAL_FRAME);
        return true;
      }
    } else if (AWT.isInternalFrameDecoration((Component) event.getSource())) {
      init(SE_DECORATION);
      return true;
    }
    return false;
  }

  @Override
  public boolean parse(AWTEvent event) {
    boolean consumed = true;
    switch (getRecordingType()) {
      case SE_INTERNAL_FRAME:
        consumed = parseInternalFrameAction(event);
        break;
      case SE_DECORATION:
        setFinished(true);
        break;
      default:
        consumed = super.parse(event);
        break;
    }
    return consumed;
  }

  protected boolean parseInternalFrameAction(AWTEvent event) {
    frame = (JInternalFrame) event.getSource();
    switch (event.getID()) {
      case ComponentEvent.COMPONENT_SHOWN:
        type = SHOW;
        break;
      case ComponentEvent.COMPONENT_HIDDEN:
        type = HIDE;
        break;
      case ComponentEvent.COMPONENT_RESIZED:
        type = RESIZE;
        break;
      case ComponentEvent.COMPONENT_MOVED:
        type = MOVE;
        break;
      case InternalFrameEvent.INTERNAL_FRAME_CLOSING:
        type = CLOSE;
        break;
      case InternalFrameEvent.INTERNAL_FRAME_ICONIFIED:
        type = ICONIFY;
        break;
      case InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED:
        type = DEICONIFY;
        break;
      default:
        throw new IllegalArgumentException("Unrecognized event: " + event);
    }
    setFinished(true);
    return true;
  }

  @Override
  protected Step createStep() {
    Step step;
    switch (getRecordingType()) {
      case SE_INTERNAL_FRAME:
        step = createInternalFrameAction(frame, type);
        break;
      case SE_DECORATION:
        step = null;
        break;
      default:
        step = super.createStep();
        break;
    }
    return step;
  }

  protected Step createInternalFrameAction(JInternalFrame target, String type) {
    ComponentReference ref = getResolver().addComponent(target);
    if (type == SHOW || type == HIDE) {
      Assert step =
          new Assert(
              getResolver(),
              null,
              ComponentTester.class.getName(),
              "assertComponentShowing",
              new String[] {ref.getID()},
              "true",
              type == HIDE);
      step.setWait(true);
      return step;
    } else if (type == MOVE) {
      Point loc = target.getLocation();
      return new Action(
          getResolver(),
          null,
          "actionMove",
          new String[] {ref.getID(), String.valueOf(loc.x), String.valueOf(loc.y)},
          JInternalFrame.class);
    } else if (type == RESIZE) {
      Dimension size = target.getSize();
      return new Action(
          getResolver(),
          null,
          "actionResize",
          new String[] {ref.getID(), String.valueOf(size.width), String.valueOf(size.height)},
          JInternalFrame.class);
    } else {
      String action =
          type == CLOSE ? "actionClose" : ((type == ICONIFY) ? "actionIconify" : "actionDeiconify");
      return new Action(
          getResolver(), null, action, new String[] {ref.getID()}, JInternalFrame.class);
    }
  }
}
