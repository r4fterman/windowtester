package abbot.tester;

import abbot.AssertionFailedError;
import abbot.Log;
import abbot.i18n.Strings;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

/**
 * Provides auto-scrolling prior to events for JComponent-derived classes.
 */
// NOTE may eventually need to push ComponentLocation up to Robot, so that
// only mousePress and actionDrop need to be overridden.  This would be mostly
// aesthetic, since making the target point visible is sufficient; having the
// entire substructure visible is just more pleasing to the eye.
// FIXME may need to override key/focus actions to scroll prior to sending the
// events, similar to how click is overridden here
public class JComponentTester extends ContainerTester {

  /**
   * This property is a duplicate of the one in JLabel, which we can't access.
   */
  private static final String LABELED_BY_PROPERTY = "labeledBy";

  public String deriveTag(Component comp) {
    // If the component class is custom, don't provide a tag
    if (isCustom(comp.getClass())) {
      return null;
    }

    JComponent jComp = ((JComponent) comp);
    String tag = null;
    // If label.setLabelFor has been used, then this component has
    // a label; use its text
    JLabel label = (JLabel) ((JComponent) comp).getClientProperty(LABELED_BY_PROPERTY);
    if (label != null && label.getText() != null && !label.getText().isEmpty()) {
      tag = label.getText();
    }
    if (tag == null || tag.equals("")) {
      AccessibleContext context = jComp.getAccessibleContext();
      tag = deriveAccessibleTag(context);
    }
    if (tag == null || tag.equals("")) {
      tag = super.deriveTag(comp);
    }
    return tag;
  }

  /**
   * Scrolls to ensure the substructure is in view before clicking.
   * @param c component
   * @param location location
   * @param buttons buttons
   * @param count count
   */
  public void actionClick(Component c, ComponentLocation location, int buttons, int count) {
    if (c instanceof JComponent) {
      scrollToVisible(c, location.getBounds(c));
    }
    super.actionClick(c, location, buttons, count);
  }

  public void actionDrag(Component c, ComponentLocation location, int modifiers) {
    if (c instanceof JComponent) {
      scrollToVisible(c, location.getBounds(c));
    }
    super.actionDrag(c, location, modifiers);
  }

  public void actionDrop(Component c, ComponentLocation location) {
    if (c instanceof JComponent) {
      scrollToVisible(c, location.getBounds(c));
    }
    super.actionDrop(c, location);
  }

  public void mousePress(Component comp, int x, int y, int buttons) {
    if (comp instanceof JComponent) {
      scrollToVisible(comp, x, y);
    }
    super.mousePress(comp, x, y, buttons);
  }

  /**
   * Scrolls the component so that the coordinate x and y are visible.  Has no effect if the component has no
   * JViewport ancestor.
   *
   * @param comp the Component to scroll
   * @param x    the x coordinate to be visible
   * @param y    the y coordinate to be visible
   */
  protected void scrollToVisible(Component comp, int x, int y) {
    Rectangle rect = new Rectangle(x, y, 1, 1);
    scrollToVisible(comp, rect);
  }

  protected void scrollRectToVisible(final JComponent jc, final Rectangle rect) {
    // Ideally, we'd use scrollbar commands to effect the scrolling,
    // but that gets really complicated for no real gain in function.
    // Fortunately, Swing's Scrollable makes for a simple solution.
    // NOTE: absolutely MUST wait for idle in order for the scroll to
    // finish, and the UI to update so that the next action goes
    // to the proper location within the scrolled component.
    invokeAndWait(
        new Runnable() {
          public void run() {
            jc.scrollRectToVisible(rect);
          }
        });
  }

  protected boolean isVisible(JComponent c, Rectangle rect) {
    Rectangle visible = c.getVisibleRect();
    return visible.contains(rect);
  }

  protected boolean isVisible(JComponent c, int x, int y) {
    Rectangle visible = c.getVisibleRect();
    return visible.contains(x, y);
  }

  /**
   * Scrolls the component so that the given rectangle is visible.  Has no effect if the component has no JViewport
   * ancestor.  When this method returns, the requested rectangle's upper left corner will be visible is required.
   *
   * @param comp the Component to scroll
   * @param rect the Rectangle to make visible.
   */
  protected void scrollToVisible(Component comp, final Rectangle rect) {
    final JComponent jc = (JComponent) comp;
    if (!isVisible(jc, rect)) {
      scrollRectToVisible(jc, rect);
      // Need to make at least the upper left corner of the requested
      // rectangle visible.
      if (!isVisible(jc, rect.x, rect.y)) {
        String msg =
            Strings.get(
                "tester.JComponent.not_visible",
                new Object[] {
                  rect.x, rect.y, jc,
                });
        throw new ActionFailedException(msg);
      }
    }
  }

  public void actionScrollToVisible(Component comp, ComponentLocation loc) {
    scrollToVisible(comp, loc.getBounds(comp));
    waitForIdle();
  }

  public void actionScrollToVisible(Component comp, int x, int y) {
    actionScrollToVisible(comp, new ComponentLocation(new Point(x, y)));
  }

  public void actionScrollToVisible(Component comp, int x, int y, int width, int height) {
    scrollToVisible(comp, new Rectangle(x, y, width, height));
    waitForIdle();
  }

  public void actionActionMap(Component comp, String name) {
    focus(comp, true);
    JComponent jc = (JComponent) comp;

    ActionMap am = jc.getActionMap();
    // On OSX/1.3.1, some action map keys are actions instead of strings.
    // On XP/1.4.1, all action map keys are strings.
    // If we can't look it up with the string key we saved, check all the
    // actions for a corresponding name.
    Object action = am.get(name);
    if (action == null) {
      Object[] keys = am.allKeys();
      for (int i = 0; keys != null && i < keys.length; i++) {
        Action value = am.get(keys[i]);
        if (value != null) {
          String aname = (String) value.getValue(Action.NAME);
          if (aname != null && aname.equals(name)) {
            action = value;
            break;
          }
        }
      }
    }
    if (action == null) {
      StringBuilder available = new StringBuilder("Available actions are the following:");
      Object[] names = am.allKeys();
      if (names != null) {
        Arrays.sort(
            names,
            (Comparator)
                (o1, o2) -> {
                  String n1 = o1.toString();
                  String n2 = o2.toString();
                  return n1.compareTo(n2);
                });
        for (Object o : names) {
          available.append("\n").append(o);
          if (!(o instanceof String)) {
            available.append(" (").append(o.getClass()).append(")");
          }
        }
      }
      throw new AssertionFailedError("No such action '" + name + "'. " + available);
    }
    InputMap im = jc.getInputMap();
    KeyStroke[] events = im.allKeys();
    for (int i = 0; events != null && i < events.length; i++) {
      KeyStroke ks = events[i];
      Object key = im.get(ks);
      // If the key is an action (OSX/1.3.1), grab the action name
      // instead
      Log.debug("ks=" + ks + " key=" + key);
      if (key instanceof Action) {
        Object nm = ((Action) key).getValue(Action.NAME);
        if (nm != null) {
          key = nm;
        }
      }
      if (name.equals(key)) {
        Log.debug("Generating keystroke " + ks + " for action " + name);
        if (ks.getKeyCode() == KeyEvent.VK_UNDEFINED) {
          keyStroke(ks.getKeyChar());
        } else {
          key(ks.getKeyCode(), ks.getModifiers());
        }
        waitForIdle();
        return;
      }
    }
    throw new ActionFailedException("No input event found for action key '" + name + "'");
  }

  public static JComponentTester getTester(JComponent c) {
    return (JComponentTester) ComponentTester.getTester(JComponent.class);
  }
}
