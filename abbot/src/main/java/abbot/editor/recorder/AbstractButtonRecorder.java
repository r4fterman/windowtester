package abbot.editor.recorder;

import abbot.script.Action;
import abbot.script.ComponentReference;
import abbot.script.Resolver;
import abbot.script.Step;
import java.awt.Component;
import java.awt.event.InputEvent;

/**
 * Record basic semantic events you might find on an AbstractButton.  This class handles a click on the button.
 */
public class AbstractButtonRecorder extends JComponentRecorder {

  public AbstractButtonRecorder(Resolver resolver) {
    super(resolver);
  }

  /**
   * Usually don't bother tracking drags/drops on buttons.
   */
  @Override
  protected boolean canDrag() {
    return false;
  }

  /**
   * Usually aren't interested in multiple clicks on a button.
   */
  @Override
  protected boolean canMultipleClick() {
    return false;
  }

  /**
   * Create a button-specific click action.
   */
  @Override
  protected Step createClick(Component target, int x, int y, int mods, int count) {
    // No need to store the coordinates, the center of the button is just
    // fine.   Only care about button 1, though.
    ComponentReference cr = getResolver().addComponent(target);
    if (mods == 0 || mods == InputEvent.BUTTON1_DOWN_MASK) {
      return new Action(
          getResolver(),
          null,
          "actionClick",
          new String[] {cr.getID()},
          javax.swing.AbstractButton.class);
    }
    return null;
  }
}
