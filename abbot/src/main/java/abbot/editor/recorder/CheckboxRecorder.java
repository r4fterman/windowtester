package abbot.editor.recorder;

import abbot.script.Action;
import abbot.script.ComponentReference;
import abbot.script.Resolver;
import abbot.script.Step;
import java.awt.Component;

/**
 * Record basic click a Checkbox component.
 */
public class CheckboxRecorder extends ComponentRecorder {

  public CheckboxRecorder(Resolver resolver) {
    super(resolver);
  }

  /**
   * Don't need to store any position or modifier information.
   */
  @Override
  protected Step createClick(Component target, int x, int y, int mods, int count) {
    ComponentReference cr = getResolver().addComponent(target);
    return new Action(getResolver(), null, "actionClick", new String[] {cr.getID()});
  }
}
