package abbot.editor.recorder;

import abbot.Log;
import abbot.script.Action;
import abbot.script.ComponentReference;
import abbot.script.Resolver;
import abbot.script.Step;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.text.JTextComponent;

/**
 * Record basic semantic events you might find on an JTextComponent.
 */
public class JTextComponentRecorder extends JComponentRecorder {

  private JTextComponent target;
  private int startIndex;
  private int endIndex;

  public static final int SE_SELECTION = 30;

  public JTextComponentRecorder(Resolver resolver) {
    super(resolver);
  }

  @Override
  protected void init(int rtype) {
    super.init(rtype);
    target = null;
    startIndex = -1;
    endIndex = -1;
  }

  /**
   * Don't store the action "default-typed"; store the key event instead.
   */
  protected boolean isMappedEvent(KeyEvent ev) {
    if (super.isMappedEvent(ev)) {
      javax.swing.Action action = getAction(ev);
      return !"default-typed".equals(action.getValue(javax.swing.Action.NAME));
    }
    return false;
  }

  /**
   * Coalesce initial click with subsequent drags to produce a selection.
   */
  @Override
  protected boolean dragStarted(
      Component target, int x, int y, int modifiers, MouseEvent dragEvent) {
    Log.debug("Tracking text selection");
    setRecordingType(SE_DROP);
    this.target = (JTextComponent) target;
    startIndex = this.target.viewToModel2D(new Point(x, y));
    // Concatenate drag/release with the original click
    return true;
  }

  @Override
  protected boolean parseDrop(AWTEvent event) {
    boolean consumed = super.parseDrop(event);
    if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
      MouseEvent me = (MouseEvent) event;
      endIndex = target.viewToModel2D(me.getPoint());
    } else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
      endIndex = target.viewToModel2D(((MouseEvent) event).getPoint());
      setFinished(true);
    }
    return consumed;
  }

  @Override
  protected Step createStep() {
    Step step;
    if (getRecordingType() == SE_DROP) {
      step = createDrop(target, startIndex, endIndex);
    } else {
      step = super.createStep();
    }
    return step;
  }

  /**
   * The text component click should click on the text index instead of a mouse coordinate.
   */
  @Override
  protected Step createClick(Component comp, int x, int y, int mods, int count) {
    if (mods == MouseEvent.BUTTON1_DOWN_MASK && count == 1) {
      ComponentReference cr = getResolver().addComponent(comp);
      JTextComponent tc = (JTextComponent) comp;
      int index = tc.viewToModel2D(new Point(x, y));
      return new Action(
          getResolver(),
          null,
          "actionClick",
          new String[] {
            cr.getID(), String.valueOf(index),
          },
          JTextComponent.class);
    } else {
      return super.createClick(comp, x, y, mods, count);
    }
  }

  @Override
  protected Step createDrop(Component comp, int start, int end) {
    ComponentReference cr = getResolver().addComponent(comp);
    return new Action(
        getResolver(),
        null,
        "actionSelectText",
        new String[] {cr.getID(), String.valueOf(start), String.valueOf(end)},
        JTextComponent.class);
  }
}
