package abbot.finder;

import abbot.ExitException;
import abbot.Log;
import abbot.tester.Robot;
import abbot.tester.WindowTracker;
import abbot.util.AWT;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

/**
 * Provides access to the current AWT hierarchy.
 */
public class AWTHierarchy implements Hierarchy {

  protected static final WindowTracker tracker = WindowTracker.getTracker();
  protected static final Collection<Component> EMPTY = new ArrayList<>();

  private static Hierarchy defaultHierarchy = null;

  public static Hierarchy getDefault() {
    return defaultHierarchy != null ? defaultHierarchy : new AWTHierarchy();
  }

  public static void setDefault(Hierarchy hierarchy) {
    defaultHierarchy = hierarchy;
  }

  @Override
  public boolean contains(Component component) {
    return true;
  }

  @Override
  public void dispose(Window window) {
    Log.debug("Dispose " + window);

    Arrays.stream(window.getOwnedWindows()).forEach(this::dispose);

    if (AWT.isSharedInvisibleFrame(window)) {
      // Don't dispose, or any child windows that may be currently
      // ignored (but not hidden) will be hidden and disposed of.
      return;
    }

    // Ensure the disposal is done on the swing thread so we can catch any
    // exceptions. If Window.dispose is called from a non-Swing thread,
    // it will invoke the dispose action on the Swing thread, but in that
    // case we have no control over exceptions.
    Runnable action =
        () -> {
          try {
            // Distinguish between the abbot framework disposing a
            // window and anyone else doing so.
            System.setProperty("abbot.finder.disposal", "true");
            window.dispose();
            System.setProperty("abbot.finder.disposal", "false");
          } catch (NullPointerException npe) {
            // Catch bug in AWT 1.3.1 when generating hierarchy
            // events
            Log.log(npe);
          } catch (ExitException e) {
            // Some apps might call System.exit on WINDOW_CLOSED
            Log.log("Ignoring SUT exit: " + e);
          } catch (Throwable e) {
            // Don't allow other exceptions to interfere with
            // disposal.
            Log.warn(e);
            Log.warn(
                "An exception was thrown when disposing "
                    + " the window "
                    + Robot.toString(window)
                    + ".  The exception is ignored");
          }
        };

    if (SwingUtilities.isEventDispatchThread()) {
      action.run();
    } else {
      try {
        SwingUtilities.invokeAndWait(action);
      } catch (InterruptedException | InvocationTargetException e) {
        // do nothing
      }
    }
  }

  @Override
  public Collection<Component> getRoots() {
    return tracker.getRootWindows();
  }

  @Override
  public Collection<Component> getComponents(Component component) {
    if (!(component instanceof Container)) {
      return EMPTY;
    }

    ArrayList<Component> list =
        new ArrayList<>(Arrays.asList(((Container) component).getComponents()));
    // Add other components which are not explicitly children, but
    // that are conceptually descendents
    switch (component) {
      case JMenu menu -> list.add(menu.getPopupMenu());
      case Window window -> list.addAll(Arrays.asList(window.getOwnedWindows()));
      case JDesktopPane jDesktopPane ->
          // Add iconified frames, which are otherwise unreachable.
          // For consistency, they are still considered children of the desktop pane.
          list.addAll(findInternalFramesFromIcons((Container) component));
      default -> {}
    }
    return list;
  }

  private Collection<Component> findInternalFramesFromIcons(Container container) {
    var list = new ArrayList<Component>();
    for (Component child : container.getComponents()) {
      if (child instanceof JInternalFrame.JDesktopIcon desktopIcon) {
        var frame = desktopIcon.getInternalFrame();
        if (frame != null) {
          list.add(frame);
        }
      } else if (child instanceof Container childContainer) {
        // OSX puts icons into a dock; handle icon manager situations here
        list.addAll(findInternalFramesFromIcons(childContainer));
      }
    }
    return list;
  }

  @Override
  public Container getParent(Component component) {
    var container = component.getParent();
    if (container == null && component instanceof JInternalFrame internalFrame) {
      // workaround for bug in JInternalFrame: COMPONENT_HIDDEN is sent
      // before the desktop icon is set, so
      // JInternalFrame.getDesktopPane will throw a NPE if called while
      // dispatching that event.  Reported against 1.4.x.
      var icon = internalFrame.getDesktopIcon();
      if (icon != null) {
        container = icon.getDesktopPane();
      }
    }
    return container;
  }
}
