package abbot.finder.matchers;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

/**
 * Provides matching of a Window by title or component name.
 */
public class WindowMatcher extends ClassMatcher {

  private final String id;
  private final boolean mustBeShowing;

  public WindowMatcher(String id) {
    this(id, true);
  }

  public WindowMatcher(String id, boolean mustBeShowing) {
    super(Window.class);
    this.id = id;
    this.mustBeShowing = mustBeShowing;
  }

  @Override
  public boolean matches(Component component) {
    return super.matches(component)
        && (component.isShowing() || !mustBeShowing)
        && (stringsMatch(id, component.getName())
            || (component instanceof Frame frame && stringsMatch(id, frame.getTitle()))
            || (component instanceof Dialog dialog && stringsMatch(id, dialog.getTitle())));
  }

  @Override
  public String toString() {
    return "Window matcher (id=" + id + ")";
  }
}
