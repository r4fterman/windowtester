package abbot.editor.recorder;

import abbot.script.Resolver;
import java.awt.AWTEvent;

/**
 * Override AbstractButton behavior, since we expect to grab a menu selection instead of a click.
 */
public class JMenuItemRecorder extends AbstractButtonRecorder {

  public JMenuItemRecorder(Resolver resolver) {
    super(resolver);
  }

  /**
   * Regular clicks get treated as a menu event.
   */
  @Override
  protected boolean isMenuEvent(AWTEvent e) {
    return isClick(e);
  }
}
