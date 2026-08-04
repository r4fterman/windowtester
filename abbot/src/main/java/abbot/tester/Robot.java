package abbot.tester;

import abbot.Log;
import abbot.Platform;
import abbot.WaitTimedOutError;
import abbot.finder.BasicFinder;
import abbot.finder.ComponentNotFoundException;
import abbot.finder.Matcher;
import abbot.finder.MultipleComponentsFoundException;
import abbot.finder.matchers.JMenuItemMatcher;
import abbot.i18n.Strings;
import abbot.script.Condition;
import abbot.util.AWT;
import abbot.util.Bugs;
import abbot.util.PerfTrace;
import abbot.util.Properties;
import abbot.util.Reflector;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.MenuBar;
import java.awt.MenuComponent;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InvocationEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleIcon;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Provide a higher level of abstraction for user input (A Better Robot). The Robot's operation may
 * be affected by the following properties:<br>
 * <pre><code>abbot.robot.auto_delay</code></pre><br>
 * Set this to a value representing the millisecond count in between generated events. Usually set
 * to 100-200 if you want to slow down the playback to simulate actual user input. The default is
 * zero delay.<br>
 * <pre><code>abbot.robot.mode</code></pre><br>
 * Set this to either "robot" or "awt" to designate the desired mode of event generation. The
 * "robot" uses java.awt.Robot to generate events, while "awt" stuffs events directly into the AWT
 * event queue.<br>
 * <pre><code>abbot.robot.event_post_delay</code></pre><br>
 * This is the maximum number of ms it takes the system to post an AWT event in response to a
 * Robot-generated event.
 * <pre><code>abbot.robot.default_delay</code></pre><br>
 * Base delay setting acts as a default value for the next two.
 * <pre><code>abbot.robot.popup_delay</code></pre><br>
 * Set this to the maximum time to wait for a menu to appear or be generated.
 * <pre><code>abbot.robot.component_delay</code></pre><br>
 * Set this to the maximum time to wait for a Component to become available.
 * <p>
 * NOTE: Only use event queue synchronization (e.g. {@link #invokeAndWait(Runnable)} or
 * {@link #waitForIdle()} when a later robot-level action is being applied to the results of a prior
 * action (e.g., focus, deiconify, menu selection).  Otherwise, don't introduce a mandatory delay
 * (e.g., use {@link #invokeLater(Runnable)}).
 * <p>
 * NOTE: If a robot action isn't reproduced properly, you may need to introduce either additional
 * events or extra delay. Adding enforced delay for a given platform is usually preferable to
 * generating additional events, so always try that first, but be sure to restrict it to the
 * platform in question.
 * <p>
 * NOTE: Robot actions should <b>never</b> be invoked on the event dispatch thread.
 */
public class Robot implements AWTConstants {

  /**
   * Use java.awt.Robot to generate events.
   */
  public static int EM_ROBOT = 0;

  /**
   * Post events to the AWT event queue.
   */
  public static int EM_AWT = 1;

  private static final Toolkit toolkit = Toolkit.getDefaultToolkit();
  private static final int MAX_DELAY = 60000;

  protected static boolean useScreenMenuBar() {
    // Ideally, we'd install a menu and check where it ended up, since the
    // property is read once at startup and ignored thereafter.
    return Platform.isOSX()
        && (Boolean.getBoolean("com.apple.macos.useScreenMenuBar")
            || Boolean.getBoolean("apple.laf.useScreenMenuBar"));
  }

  /**
   * Base delay setting.
   */
  public static int defaultDelay =
      Properties.getProperty("abbot.robot.default_delay", 30000, 0, 60000);

  /**
   * Delay before checking for idle.  This allows the system a little time to put a native event
   * onto the AWT event queue.
   */
  private static int eventPostDelay =
      Properties.getProperty("abbot.robot.event_post_delay", 100, 0, 1000);

  protected static long IDLE_TIMEOUT = Integer.getInteger("abbot.robot.idle_timeout", 10000);

  /**
   * Delay before failing to find a popup menu that should appear.
   */
  protected static int popupDelay =
      Properties.getProperty("abbot.robot.popup_delay", defaultDelay, 0, 60000);

  /**
   * Delay before failing to find a component that should be visible.
   */
  public static int componentDelay =
      Properties.getProperty("abbot.robot.component_delay", defaultDelay, 0, 60000);

  /**
   * With decreased robot auto delay, OSX popup menus don't activate properly.  Indicate the minimum
   * delay for proper operation (determined experimentally).
   */
  private static final int subMenuDelay = Platform.isOSX() ? 100 : 0;

  /**
   * How events are generated.
   */
  private static int eventMode = EM_ROBOT;

  private static boolean verified = false;
  private static boolean serviceMode = false;

  // FIXME add one per graphics device?
  /**
   * The robot used to generate events.
   */
  private static final java.awt.Robot robot;

  private static final WindowTracker tracker;

  /**
   * Current input state.  This will either be that of the AWT event queue or of the robot,
   * depending on the dispatch mode. Note that the robot state may be different from that seen by
   * the AWT event queue, since robot events may be as yet unprocessed.
   */
  private static final InputState state;

  /**
   * Suitable inter-event delay for most cases; tests have been run safely at this value.  Should
   * definitely be less than the double-click threshold.<p>
   */
  private static final int DEFAULT_DELAY = getPreferredRobotAutoDelay();

  private static final int SLEEP_INTERVAL = 10;

  private static int autoDelay = DEFAULT_DELAY;

  public static int getAutoDelay() {
    return autoDelay;
  }

  public static java.awt.Robot getRobot() {
    return serviceMode ? null : robot;
  }

  public static InputState getState() {
    return state;
  }

  static {
    robot = createRobot();
    tracker = WindowTracker.getTracker();
    state = new InputState();
  }

  private static java.awt.Robot createRobot() {
    java.awt.Robot robot = null;
    String mode = System.getProperty("abbot.robot.mode", "robot");
    autoDelay = Properties.getProperty("abbot.robot.auto_delay", autoDelay, -1, 60000);
    try {
      // Even if the robot doesn't work, we can still use it for some
      // things.
      robot = new java.awt.Robot();
      if (autoDelay != -1) {
        robot.setAutoDelay(autoDelay);
      } else {
        autoDelay = robot.getAutoDelay();
      }
      if (!verified) {
        verified = true;
        boolean skip = "false".equals(System.getProperty("abbot.robot.verify"));
        if (!skip && !RobotVerifier.verify(robot)) {
          // robot doesn't work (w32 service mode)
          serviceMode = true;
          System.err.println("Robot non-functional, " + "falling back to AWT mode");
          mode = "awt";
        }
      }
    } catch (AWTException e) {
      // no robot available, send AWT events
      System.err.println("Falling back to AWT mode: " + e.getMessage());
      mode = "awt";
    }
    if (mode.equals("awt")) {
      eventMode = EM_AWT;
    }
    return robot;
  }

  public static int getEventMode() {
    return eventMode;
  }

  public static String getEventModeDescription() {
    String desc = eventMode == EM_ROBOT ? "robot" : "awt";
    if (serviceMode) {
      desc += " (service)";
    }
    return desc;
  }

  public static void setEventMode(int mode) {
    if (eventMode != mode) {
      if (mode == EM_ROBOT && (serviceMode || robot == null)) {
        String msg = Strings.get("tester.Robot.no_robot_mode");
        throw new IllegalStateException(msg);
      }
      eventMode = mode;
    }
  }

  public static int getEventPostDelay() {
    return eventPostDelay;
  }

  public static void setEventPostDelay(int delay) {
    eventPostDelay = Math.min(1000, Math.max(0, delay));
  }

  public static void setAutoDelay(int ms) {
    ms = Math.min(60000, Math.max(0, ms));
    if (eventMode == EM_ROBOT) {
      robot.setAutoDelay(ms);
    }
    autoDelay = ms;
    Log.debug("Auto delay set to " + ms);
  }

  public void mouseMove(int x, int y) {
    if (eventMode == EM_ROBOT) {
      Log.debug("ROBOT: Mouse move: (" + x + "," + y + ")");
      robot.mouseMove(x, y);
    }
  }

  public void mousePress(int buttons) {
    if (eventMode == EM_ROBOT) {
      Log.debug("ROBOT: Mouse press: " + AWT.getMouseModifiers(buttons));
      // OSX 1.4.1 accidentally swaps mb2 and mb3; fix it here
      robot.mousePress(buttons);
    } else {
      Component c = state.getMouseComponent();
      if (c == null) {
        Log.warn("No current mouse component for button press", 4);
        return;
      }
      Point where = state.getMouseLocation();
      postMousePress(c, where.x, where.y, buttons);
    }
  }

  public void mouseRelease() {
    mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);
  }

  public void mouseRelease(int buttons) {
    if (eventMode == EM_ROBOT) {
      Log.debug("ROBOT: Mouse release: " + AWT.getMouseModifiers(buttons));
      robot.mouseRelease(buttons);
    } else {
      Component source =
          state.isDragging()
              ? state.getDragSource()
              : (lastMousePress != null
                  ? lastMousePress.getComponent()
                  : state.getMouseComponent());
      Point where = state.getMouseLocation();
      if (source == null) {
        Log.warn("Mouse release outside of available frames");
        return;
      } else if (where == null) {
        if (lastMousePress == null) {
          Log.warn("No record of most recent mouse press");
          return;
        }
        where = lastMousePress.getPoint();
      }
      postMouseRelease(source, where.x, where.y, buttons);
    }
  }

  public void focus(Component comp) {
    focus(comp, false);
  }

  /**
   * Use an explicit listener, since hasFocus is not always reliable.
   */
  private static class FocusWatcher extends FocusAdapter {

    public volatile boolean focused = false;

    public FocusWatcher(Component c) {
      c.addFocusListener(this);
      focused = AWT.getFocusOwner() == c;
    }

    @Override
    public void focusGained(FocusEvent f) {
      focused = true;
    }

    @Override
    public void focusLost(FocusEvent f) {
      focused = false;
    }
  }

  public void focus(Component comp, boolean wait) {
    Component currentOwner = AWT.getFocusOwner();
    if (currentOwner == comp) {
      return;
    }
    Log.debug("Focus change");
    FocusWatcher fw = new FocusWatcher(comp);
    // for pointer focus
    mouseMove(comp, comp.getWidth() / 2, comp.getHeight() / 2);
    waitForIdle();
    // Make sure the correct window is in front
    Window w1 = currentOwner != null ? AWT.getWindow(currentOwner) : null;
    Window w2 = AWT.getWindow(comp);
    if (w1 != w2) {
      activate(w2);
      waitForIdle();
    }
    // NOTE: while it would be nice to have a robot method instead of
    // requesting focus, clicking to change focus may have
    // side effects
    invokeAndWait(comp, comp::requestFocus);
    try {
      if (wait) {
        long start = System.currentTimeMillis();
        while (!fw.focused) {
          if (System.currentTimeMillis() - start > componentDelay) {
            String msg = Strings.get("tester.Robot.focus_failed", new Object[] {toString(comp)});
            throw new ActionFailedException(msg);
          }
          sleep();
        }
      }
    } finally {
      comp.removeFocusListener(fw);
    }
  }

  protected EventQueue getEventQueue(Component c) {
    return c != null ? tracker.getQueue(c) : toolkit.getSystemEventQueue();
  }

  public void invokeLater(Component context, Runnable action) {
    EventQueue queue = getEventQueue(context);
    queue.postEvent(new InvocationEvent(toolkit, action));
  }

  public void invokeAndWait(Component c, Runnable action) {
    invokeLater(c, action);
    waitForIdle();
  }

  public void invokeLater(Runnable action) {
    invokeLater(null, action);
  }

  public void invokeAndWait(Runnable action) {
    invokeAndWait(null, action);
  }

  public void keyPress(int keycode) {
    keyPress(keycode, KeyEvent.CHAR_UNDEFINED);
  }

  /**
   * Send a key press event.
   */
  private void keyPress(int keycode, char keyChar) {
    if (eventMode == EM_ROBOT) {
      Log.debug("ROBOT: key press " + AWT.getKeyCode(keycode));
      robot.keyPress(keycode);
    } else {
      int mods = state.getModifiers();
      if (AWT.isModifier(keycode)) {
        mods |= AWT.keyCodeToMask(keycode);
      }
      postKeyEvent(KeyEvent.KEY_PRESSED, mods, keycode, KeyEvent.CHAR_UNDEFINED);
      // Auto-generate KEY_TYPED events as best we can
      int mask = state.getModifiers();
      if (keyChar == KeyEvent.CHAR_UNDEFINED) {
        KeyStroke ks = KeyStroke.getKeyStroke(keycode, mask);
        keyChar = KeyStrokeMap.getChar(ks);
      }
      if (keyChar != KeyEvent.CHAR_UNDEFINED) {
        postKeyEvent(KeyEvent.KEY_TYPED, mask, KeyEvent.VK_UNDEFINED, keyChar);
      }
    }
  }

  public void keyRelease(int keycode) {
    if (eventMode == EM_ROBOT) {
      Log.debug("ROBOT: key release " + AWT.getKeyCode(keycode));
      robot.keyRelease(keycode);
      if (Bugs.hasKeyInputDelay()) {
        // OSX, empirical
        int KEY_INPUT_DELAY = 200;
        if (KEY_INPUT_DELAY > autoDelay) {
          delay(KEY_INPUT_DELAY - autoDelay);
        }
      }
    } else {
      int mods = state.getModifiers();
      if (AWT.isModifier(keycode)) {
        mods &= ~AWT.keyCodeToMask(keycode);
      }
      postKeyEvent(KeyEvent.KEY_RELEASED, mods, keycode, KeyEvent.CHAR_UNDEFINED);
    }
  }

  private void postKeyEvent(int id, int modifiers, int keycode, char ch) {
    Component c = findFocusOwner();
    if (c != null) {
      postEvent(c, new KeyEvent(c, id, System.currentTimeMillis(), modifiers, keycode, ch));
    } else {
      Log.warn("No component has focus, key press discarded");
    }
  }

  /**
   * Sleep for a little bit, measured in UI time.
   */
  public void sleep() {
    delay(SLEEP_INTERVAL);
  }

  public void delay(int ms) {
    if (eventMode == EM_ROBOT) {
      while (ms > MAX_DELAY) {
        robot.delay(MAX_DELAY);
        ms -= MAX_DELAY;
      }
      robot.delay(ms);
    } else {
      try {
        Thread.sleep(ms);
      } catch (InterruptedException ie) {
        // ignore
      }
    }
  }

  private static final Runnable EMPTY_RUNNABLE = () -> {};

  /**
   * Check for a blocked event queue (symptomatic of an active w32 AWT popup menu).
   *
   * @return whether the event queue is blocked.
   */
  protected boolean queueBlocked() {
    return postInvocationEvent(toolkit.getSystemEventQueue(), 200);
  }

  protected boolean postInvocationEvent(EventQueue eq, long timeout) {
    Object lock = new RobotIdleLock();
    synchronized (lock) {
      eq.postEvent(new InvocationEvent(Robot.toolkit, EMPTY_RUNNABLE, lock, true));
      long start = System.currentTimeMillis();
      try {
        // NOTE: on fast linux systems when showing a dialog, if we
        // don't provide a timeout, we're never notified, and the
        // test will wait forever (up through 1.5.0_05).
        lock.wait(timeout);
        return (System.currentTimeMillis() - start) >= IDLE_TIMEOUT;
      } catch (InterruptedException e) {
        Log.warn("Invocation lock interrupted");
      }
    }
    return false;
  }

  private void waitForIdle(EventQueue eq) {
    if (EventQueue.isDispatchThread()) {
      throw new IllegalThreadStateException("Cannot call method from the event dispatcher thread");
    }

    // NOTE: as of Java 1.3.1, robot.waitForIdle only waits for the
    // last event on the queue at the time of this invocation to be
    // processed. We need it better than that. Make sure the given event
    // queue is empty when this method returns

    // We always post at least one idle event to allow any current event
    // dispatch processing to finish.
    long start = System.currentTimeMillis();
    int count = 0;
    do {
      if (postInvocationEvent(eq, IDLE_TIMEOUT)) {
        Log.warn(
            "Timed out waiting for posted invocation event: "
                + IDLE_TIMEOUT
                + "ms (after "
                + count
                + " events)",
            Log.FULL_STACK);
        break;
      }
      if (System.currentTimeMillis() - start > IDLE_TIMEOUT) {
        Log.warn("Timed out waiting for idle event queue after " + count + " events");
        break;
      }

      ++count;

      // NOTE: this does not detect invocation events (e.g., what
      // gets posted with EventQueue.invokeLater), so if someone
      // is repeatedly posting one, we might get stuck.  Not too
      // worried, since if a Runnable keeps calling invokeLater
      // on itself, *nothing* else gets much chance to run, so it
      // seems to be a bad programming practice.
    } while (eq.peekEvent() != null);
  }

  /**
   * Wait for an idle AWT event queue.  Note that this is different from the implementation of
   * <code>java.awt.Robot.waitForIdle()</code>, which may have events on the queue when it returns.
   * Do <b>NOT</b> use this method if there are animations or other continual refreshes happening,
   * since in that case it may never return.
   */
  public void waitForIdle() {
    long perfStart = PerfTrace.start();
    if (eventPostDelay > autoDelay) {
      delay(eventPostDelay - autoDelay);
    }
    Collection<EventQueue> queues = tracker.getEventQueues();
    if (queues.size() == 1) {
      waitForIdle(toolkit.getSystemEventQueue());
    } else {
      // FIXME this resurrects dead event queues
      for (EventQueue eq : queues) {
        waitForIdle(eq);
      }
    }
    PerfTrace.end(perfStart, "Robot.waitForIdle");
  }

  public Color sample(int x, int y) {
    // Service mode always returns black when sampled
    if (robot != null && !serviceMode) {
      return robot.getPixelColor(x, y);
    }
    String msg = Strings.get("tester.Robot.no_sample");
    throw new UnsupportedOperationException(msg);
  }

  public Color sample(Component c, int x, int y) {
    Point p = AWT.getLocationOnScreen(c);
    return sample(p.x + x, p.y + y);
  }

  // NOTE: Text components (and maybe others with a custom cursor) will
  // capture the cursor.  May want to move the cursor out of the component
  // bounds, although this might cause issues where the component is
  // responding visually to mouse movement.
  // Is this an OSX bug?
  //
  public BufferedImage capture(Rectangle bounds) {
    return robot != null ? robot.createScreenCapture(bounds) : null;
  }

  public BufferedImage capture(Component comp) {
    return capture(comp, true);
  }

  public BufferedImage capture(Component comp, boolean ignoreBorder) {
    Rectangle bounds = new Rectangle(comp.getSize());
    Point loc = AWT.getLocationOnScreen(comp);
    bounds.x = loc.x;
    bounds.y = loc.y;
    Log.debug("Component bounds " + bounds);
    // Make sure we don't sample the decorating space
    if (ignoreBorder && (comp instanceof Container)) {
      Insets insets = ((Container) comp).getInsets();
      bounds.x += insets.left;
      bounds.y += insets.top;
      bounds.width -= insets.left + insets.right;
      bounds.height -= insets.top + insets.bottom;
      Log.debug("Component insets " + insets);
      Log.debug("Component bounds " + bounds);
    }
    return capture(bounds);
  }

  // Bug workaround support
  protected void jitter(Component comp, int x, int y) {
    mouseMove(comp, (x > 0 ? x - 1 : x + 1), y);
  }

  // Bug workaround support
  protected void jitter(int x, int y) {
    mouseMove((x > 0 ? x - 1 : x + 1), y);
  }

  public void mouseMove(Component comp) {
    mouseMove(comp, comp.getWidth() / 2, comp.getHeight() / 2);
  }

  /**
   * Wait the given number of ms for the component to be showing and ready.  Returns false if the
   * operation times out.
   */
  private boolean notWaitForComponent(Component c, long delay) {
    if (!isReadyForInput(c)) {
      Log.debug("Waiting for component to show");
      long start = System.currentTimeMillis();
      while (!isReadyForInput(c)) {
        if (c instanceof JPopupMenu) {
          // wiggle the mouse over the parent menu item to
          // ensure the submenu shows
          Component invoker = ((JPopupMenu) c).getInvoker();
          if (invoker instanceof JMenu) {
            jitter(invoker, invoker.getWidth() / 2, invoker.getHeight() / 2);
          }
        }
        if (System.currentTimeMillis() - start > delay) {
          Log.warn(
              "Component "
                  + toString(c)
                  + " ("
                  + Integer.toHexString(c.hashCode())
                  + ")"
                  + " not ready after "
                  + delay
                  + "ms: "
                  + "showing="
                  + c.isShowing()
                  + " win ready="
                  + tracker.isWindowReady(AWT.getWindow(c)));
          return true;
        }
        sleep();
      }
    }
    return false;
  }

  /**
   * If a component does not have mouse events enabled, use the first ancestor which does.
   */
  private Component retargetMouseEvent(Component comp, int id, Point pt) {
    Point where = pt;
    while (!(comp instanceof Window) && !AWT.eventTypeEnabled(comp, id)) {
      Log.debug("Retargeting event, " + toString(comp) + " not interested");
      where = SwingUtilities.convertPoint(comp, where.x, where.y, comp.getParent());
      comp = comp.getParent();
    }
    pt.setLocation(where);
    return comp;
  }

  public void mouseMove(Component comp, int x, int y) {
    if (notWaitForComponent(comp, componentDelay)) {
      String msg = "Can't obtain position of component " + toString(comp);
      throw new ComponentNotShowingException(msg);
    }
    if (eventMode == EM_ROBOT) {
      try {
        Point point = AWT.getLocationOnScreen(comp);
        point.translate(x, y);
        mouseMove(point.x, point.y);
      } catch (java.awt.IllegalComponentStateException e) {
        // ignore
      }
    } else {
      Component eventSource;
      int id = MouseEvent.MOUSE_MOVED;
      boolean outside = false;

      // When dragging, the event source is always the target of the
      // original mouse press.
      if (state.isDragging()) {
        id = MouseEvent.MOUSE_DRAGGED;
        eventSource = state.getDragSource();
      } else {
        Point pt = new Point(x, y);
        eventSource = comp = retargetMouseEvent(comp, id, pt);
        x = pt.x;
        y = pt.y;
        outside = x < 0 || y < 0 || x >= comp.getWidth() || y >= comp.getHeight();
      }

      Component current = state.getMouseComponent();
      if (current != comp) {
        if (outside && current != null) {
          Point pt = SwingUtilities.convertPoint(comp, x, y, current);
          postMouseMotion(current, MouseEvent.MOUSE_EXITED, pt);
          return;
        }
        postMouseMotion(comp, MouseEvent.MOUSE_ENTERED, new Point(x, y));
      }
      Point pt = new Point(x, y);
      if (id == MouseEvent.MOUSE_DRAGGED) {
        // Drag coordinates are relative to drag source component
        pt = SwingUtilities.convertPoint(comp, pt, eventSource);
      }
      postMouseMotion(eventSource, id, pt);
      // Add an exit event if warranted
      if (outside) {
        postMouseMotion(comp, MouseEvent.MOUSE_EXITED, new Point(x, y));
      }
    }
  }

  public void dragOver(Component dst, int x, int y) {
    mouseMove(dst, x - 4, y);
    mouseMove(dst, x, y);
  }

  public void drag(Component src, int sx, int sy) {
    drag(src, sx, sy, InputEvent.BUTTON1_DOWN_MASK);
  }

  // TODO: maybe auto-switch to robot mode if available?
  public void drag(Component src, int sx, int sy, int buttons) {
    if (Bugs.dragDropRequiresNativeEvents()
        && eventMode != EM_ROBOT
        && !Boolean.getBoolean("abbot.ignore_drag_error")) {
      String msg = Strings.get("abbot.Robot.no_drag_available");
      if (serviceMode) {
        // If we start a native drag in this mode, it'll pretty much
        // lock up the system, apparently with the native AWT libs
        // starting a thread invisible to the VM that chews up all CPU
        // time.
        throw new ActionFailedException(msg);
      }
      Log.warn(msg);
    }

    // Some platforms require a pause between mouse down and mouse motion
    int DRAG_DELAY =
        Properties.getProperty(
            "abbot.robot.drag_delay", Platform.isX11() || Platform.isOSX() ? 100 : 0, 0, 60000);

    mousePress(src, sx, sy, buttons);
    if (DRAG_DELAY > autoDelay) {
      delay(DRAG_DELAY);
    }
    if (Platform.isWindows() || Platform.isMacintosh()) {
      int dx = sx + AWTConstants.DRAG_THRESHOLD < src.getWidth() ? AWTConstants.DRAG_THRESHOLD : 0;
      int dy = sy + AWTConstants.DRAG_THRESHOLD < src.getHeight() ? AWTConstants.DRAG_THRESHOLD : 0;
      if (dx == 0 && dy == 0) {
        dx = AWTConstants.DRAG_THRESHOLD;
      }
      mouseMove(src, sx + dx / 4, sy + dy / 4);
      mouseMove(src, sx + dx / 2, sy + dy / 2);
      mouseMove(src, sx + dx, sy + dy);
      mouseMove(src, sx + dx + 1, sy + dy);
    } else {
      mouseMove(src, sx + AWTConstants.DRAG_THRESHOLD / 2, sy + AWTConstants.DRAG_THRESHOLD / 2);
      mouseMove(src, sx + AWTConstants.DRAG_THRESHOLD, sy + AWTConstants.DRAG_THRESHOLD);
      mouseMove(src, sx + AWTConstants.DRAG_THRESHOLD / 2, sy + AWTConstants.DRAG_THRESHOLD / 2);
      mouseMove(src, sx, sy);
    }
  }

  public void drop(Component target, int x, int y) {
    // Delay between final move and drop to ensure the drop ends.
    int DROP_DELAY =
        Properties.getProperty("abbot.robot.drop_delay", Platform.isWindows() ? 200 : 0, 0, 60000);

    // All motion events are relative to the drag source
    long start = System.currentTimeMillis();
    while (!state.isDragging()) {
      if (System.currentTimeMillis() - start > eventPostDelay) {
        String msg = Strings.get("Robot.no_current_drag");
        throw new ActionFailedException(msg);
      }
      sleep();
    }
    dragOver(target, x, y);
    if (DROP_DELAY > autoDelay) {
      delay(DROP_DELAY - autoDelay);
    }

    mouseRelease(state.getButtons());
  }

  /**
   * Generate a mouse enter/exit/move/drag for the destination component. NOTE: The VM automatically
   * usually generates exit events; need a test to define the behavior, though.
   */
  private void postMouseMotion(Component dst, int id, Point to) {
    // The VM auto-generates exit events as needed (1.3, 1.4)
    if (id != MouseEvent.MOUSE_DRAGGED) {
      dst = retargetMouseEvent(dst, id, to);
    }
    // Avoid multiple moves to the same location
    if (state.getMouseComponent() != dst || !to.equals(state.getMouseLocation())) {
      postEvent(
          dst,
          new MouseEvent(
              dst,
              id,
              System.currentTimeMillis(),
              state.getModifiers(),
              to.x,
              to.y,
              state.getClickCount(),
              false));
    }
  }

  public void key(int keycode) {
    key(keycode, 0);
  }

  public void setModifiers(int modifiers, boolean press) {
    boolean altGraph = (modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0;
    boolean shift = (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
    boolean alt = (modifiers & InputEvent.ALT_DOWN_MASK) != 0;
    boolean ctrl = (modifiers & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0;
    boolean meta = (modifiers & InputEvent.META_DOWN_MASK) != 0;
    if (press) {
      if (altGraph) {
        keyPress(KeyEvent.VK_ALT_GRAPH);
      }
      if (alt) {
        keyPress(KeyEvent.VK_ALT);
      }
      if (shift) {
        keyPress(KeyEvent.VK_SHIFT);
      }
      if (ctrl) {
        keyPress(KeyEvent.VK_CONTROL);
      }
      if (meta) {
        keyPress(KeyEvent.VK_META);
      }
    } else {
      // For consistency, release in the reverse order of press
      if (meta) {
        keyRelease(KeyEvent.VK_META);
      }
      if (ctrl) {
        keyRelease(KeyEvent.VK_CONTROL);
      }
      if (shift) {
        keyRelease(KeyEvent.VK_SHIFT);
      }
      if (alt) {
        keyRelease(KeyEvent.VK_ALT);
      }
      if (altGraph) {
        keyRelease(KeyEvent.VK_ALT_GRAPH);
      }
    }
  }

  public void key(int keycode, int modifiers) {
    key(KeyEvent.CHAR_UNDEFINED, keycode, modifiers);
  }

  private void key(char ch, int keycode, int modifiers) {
    Log.debug("key keycode=" + AWT.getKeyCode(keycode) + " mod=" + AWT.getKeyModifiers(modifiers));
    boolean isModifier = true;
    switch (keycode) {
      case KeyEvent.VK_ALT_GRAPH:
        modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
        break;
      case KeyEvent.VK_ALT:
        modifiers |= InputEvent.ALT_DOWN_MASK;
        break;
      case KeyEvent.VK_SHIFT:
        modifiers |= InputEvent.SHIFT_DOWN_MASK;
        break;
      case KeyEvent.VK_CONTROL:
        modifiers |= Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        break;
      case KeyEvent.VK_META:
        modifiers |= InputEvent.META_DOWN_MASK;
        break;
      default:
        isModifier = false;
        break;
    }
    setModifiers(modifiers, true);
    if (!isModifier) {
      keyPress(keycode, ch);
      keyRelease(keycode);
    }
    setModifiers(modifiers, false);
    if (Bugs.hasKeyStrokeGenerationBug()) {
      delay(100);
    }
  }

  // FIXME should this be renamed to "key"?
  public void keyStroke(char ch) {
    KeyStroke ks = KeyStrokeMap.getKeyStroke(ch);
    if (ks == null) {
      // If no mapping is available, we omit press/release events and
      // only generate a KEY_TYPED event
      Log.debug("No key mapping for '" + ch + "'");
      Component focus = findFocusOwner();
      if (focus == null) {
        Log.warn("No component has focus, keystroke discarded", Log.FULL_STACK);
        return;
      }
      KeyEvent ke =
          new KeyEvent(
              focus, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, ch);
      // Allow any pending robot events to complete; otherwise we
      // might stuff the typed event before previous robot-generated
      // events are posted.
      if (eventMode == EM_ROBOT) {
        waitForIdle();
      }
      postEvent(focus, ke);
    } else {
      int keycode = ks.getKeyCode();
      int mod = ks.getModifiers();
      Log.debug("Char '" + ch + "' generated by keycode=" + keycode + " mod=" + mod);
      key(ch, keycode, mod);
    }
  }

  public void keyString(String str) {
    char[] ch = str.toCharArray();
    for (char c : ch) {
      keyStroke(c);
    }
  }

  public void mousePress(Component comp) {
    mousePress(comp, InputEvent.BUTTON1_DOWN_MASK);
  }

  public void mousePress(Component comp, int mask) {
    mousePress(comp, comp.getWidth() / 2, comp.getHeight() / 2, mask);
  }

  public void mousePress(Component comp, int x, int y) {
    mousePress(comp, x, y, InputEvent.BUTTON1_DOWN_MASK);
  }

  public void mousePress(Component comp, int x, int y, int mask) {
    if (eventMode == EM_ROBOT && Bugs.hasRobotMotionBug()) {
      jitter(comp, x, y);
    }
    mouseMove(comp, x, y);
    if (eventMode == EM_ROBOT) {
      mousePress(mask);
    } else {
      postMousePress(comp, x, y, mask);
    }
  }

  /**
   * Post a mouse press event to the AWT event queue for the given component.
   */
  private void postMousePress(Component comp, int x, int y, int mask) {
    long when = lastMousePress != null ? lastMousePress.getWhen() : 0;
    long now = System.currentTimeMillis();
    int count = 1;
    Point where = new Point(x, y);
    comp = retargetMouseEvent(comp, MouseEvent.MOUSE_PRESSED, where);
    if (countingClicks && comp == lastMousePress.getComponent()) {
      long delta = now - when;
      if (delta < AWTConstants.MULTI_CLICK_INTERVAL) {
        count = state.getClickCount() + 1;
      }
    }
    postEvent(
        comp,
        new MouseEvent(
            comp,
            MouseEvent.MOUSE_PRESSED,
            now,
            state.getKeyModifiers() | mask,
            where.x,
            where.y,
            count,
            AWTConstants.POPUP_ON_PRESS && (mask & AWTConstants.POPUP_MASK) != 0));
  }

  /**
   * Post a mouse release event to the AWT event queue for the given component.
   */
  private void postMouseRelease(Component c, int x, int y, int mask) {
    long now = System.currentTimeMillis();
    int count = state.getClickCount();
    Point where = new Point(x, y);
    c = retargetMouseEvent(c, MouseEvent.MOUSE_PRESSED, where);
    postEvent(
        c,
        new MouseEvent(
            c,
            MouseEvent.MOUSE_RELEASED,
            now,
            state.getKeyModifiers() | mask,
            where.x,
            where.y,
            count,
            !AWTConstants.POPUP_ON_PRESS && (mask & AWTConstants.POPUP_MASK) != 0));
  }

  public final void click(Component comp) {
    click(comp, comp.getWidth() / 2, comp.getHeight() / 2);
  }

  public final void click(Component comp, int mask) {
    click(comp, comp.getWidth() / 2, comp.getHeight() / 2, mask);
  }

  public final void click(Component comp, int x, int y) {
    click(comp, x, y, InputEvent.BUTTON1_DOWN_MASK);
  }

  public final void click(Component comp, int x, int y, int mask) {
    click(comp, x, y, mask, 1);
  }

  public void click(Component comp, int x, int y, int mask, int count) {
    var message =
        "Click at ("
            + x
            + ","
            + y
            + ") on "
            + toString(comp)
            + (count > 1 ? (" count=" + count) : "");
    Log.debug(message);

    int keyModifiers = mask & ~AWTConstants.BUTTON_DOWN_MASK;
    mask &= AWTConstants.BUTTON_DOWN_MASK;
    setModifiers(keyModifiers, true);
    // Adjust the auto-delay to ensure we actually get a multiple click
    // In general clicks have to be less than 200ms apart, although the
    // actual setting is not readable by java that I'm aware of.
    int oldDelay = getAutoDelay();
    if (count > 1 && oldDelay * 2 > 200) {
      setAutoDelay(0);
    }
    long last = System.currentTimeMillis();
    mousePress(comp, x, y, mask);
    while (count-- > 1) {
      mouseRelease(mask);
      long delta = System.currentTimeMillis() - last;
      if (delta > AWTConstants.MULTI_CLICK_INTERVAL) {
        Log.warn("Unexpected delay in multi-click: " + delta);
      }
      last = System.currentTimeMillis();
      mousePress(mask);
    }
    setAutoDelay(oldDelay);
    mouseRelease(mask);
    setModifiers(keyModifiers, false);
  }

  public void selectAWTMenuItem(Frame frame, String path) {
    MenuBar mb = frame.getMenuBar();
    if (mb == null) {
      String msg = Strings.get("tester.Robot.no_menu_bar", new Object[] {toString(frame)});
      throw new ActionFailedException(msg);
    }
    MenuItem[] items = AWT.findAWTMenuItems(frame, path);
    if (items.length == 0) {
      String msg = Strings.get("tester.Robot.no_menu_item", new Object[] {path, toString(frame)});
      throw new ActionFailedException(msg);
    }
    if (items.length > 1) {
      String msg = Strings.get("tester.Robot.multiple_menu_items");
      throw new ActionFailedException(msg);
    }
    selectAWTMenuItem(items[0]);
  }

  public void selectAWTPopupMenuItem(Component invoker, String path) {
    try {
      PopupMenu[] popups = AWT.getPopupMenus(invoker);
      if (popups.length == 0) {
        throw new ActionFailedException(Strings.get("tester.Robot.awt_popup_missing"));
      }

      MenuItem[] items = AWT.findAWTPopupMenuItems(invoker, path);
      if (items.length == 1) {
        selectAWTPopupMenuItem(items[0]);
        return;
      } else if (items.length == 0) {
        String msg =
            Strings.get("tester.Robot.no_popup_menu_item", new Object[] {path, toString(invoker)});
        throw new ActionFailedException(msg);
      }
      String msg = Strings.get("tester.Robot.multiple_menu_items", new Object[] {path});
      throw new ActionFailedException(msg);
    } finally {
      AWT.dismissAWTPopup();
    }
  }

  protected void fireAccessibleAction(Component context, AccessibleAction action, String name) {
    if (action != null && action.getAccessibleActionCount() > 0) {
      invokeLater(context, () -> action.doAccessibleAction(0));
    } else {
      String msg = Strings.get("tester.Robot.no_accessible_action", new String[] {name});
      throw new ActionFailedException(msg);
    }
  }

  private Component getContext(MenuComponent item) {
    while (!(item.getParent() instanceof Component) && item.getParent() instanceof MenuComponent) {
      item = (MenuComponent) item.getParent();
    }
    return (Component) item.getParent();
  }

  public void selectAWTMenuItem(MenuComponent item) {
    // Can't do this through coordinates because MenuComponents don't
    // store any of that information
    fireAccessibleAction(
        getContext(item), item.getAccessibleContext().getAccessibleAction(), toString(item));
    if (queueBlocked()) {
      key(KeyEvent.VK_ESCAPE);
    }
  }

  public void selectAWTPopupMenuItem(MenuComponent item) {
    // Can't do this through coordinates because MenuComponents don't
    // store any of that information
    fireAccessibleAction(
        getContext(item), item.getAccessibleContext().getAccessibleAction(), toString(item));
    if (queueBlocked()) {
      key(KeyEvent.VK_ESCAPE);
    }
  }

  protected boolean isReadyForInput(Component c) {
    if (eventMode == EM_AWT) {
      return c.isShowing();
    }
    return c.isShowing() && tracker.isWindowReady(AWT.getWindow(c));
  }

  private boolean isOnJMenuBar(Component item) {
    if (item instanceof javax.swing.JMenuBar) {
      return true;
    }
    Component parent =
        item instanceof JPopupMenu ? ((JPopupMenu) item).getInvoker() : item.getParent();
    return parent != null && isOnJMenuBar(parent);
  }

  public void selectMenuItem(Component sameWindow, String path) {
    try {
      Window window = AWT.getWindow(sameWindow);
      Matcher m = new JMenuItemMatcher(path);
      Component item = BasicFinder.getDefault().find(window, m);
      selectMenuItem(item);
    } catch (ComponentNotFoundException e) {
      throw new ComponentMissingException("Can't find menu item '" + path + "'");
    } catch (MultipleComponentsFoundException e) {
      throw new ActionFailedException(e.getMessage());
    }
  }

  public void selectMenuItem(Component item) {
    Log.debug("Selecting menu item " + toString(item));
    Component parent = item.getParent();
    JPopupMenu parentPopup = null;
    if (parent instanceof JPopupMenu) {
      parentPopup = (JPopupMenu) parent;
      parent = ((JPopupMenu) parent).getInvoker();
    }
    boolean inMenuBar = parent instanceof javax.swing.JMenuBar;
    boolean isMenu = item instanceof javax.swing.JMenu;

    if (isOnJMenuBar(item) && useScreenMenuBar()) {
      // Use accessibility action instead
      fireAccessibleAction(item, item.getAccessibleContext().getAccessibleAction(), toString(item));
      return;
    }

    // If our parent is a menu, activate it first if it's not already.
    if (parent instanceof javax.swing.JMenuItem) {
      if (parentPopup == null || !parentPopup.isShowing()) {
        Log.debug("Opening parent menu " + toString(parent));
        selectMenuItem(parent);
      }
    }

    // Make sure the appropriate window is in front
    if (inMenuBar) {
      Window win = AWT.getWindow(parent);
      if (win != null) {
        // Make sure the window is in front, or its menus may be
        // obscured by another window.
        invokeAndWait(win, win::toFront);
        mouseMove(win);
      }
    }

    // Activate the item
    if (isMenu && !inMenuBar) {
      // Submenus only require a mouse-over to activate, but do
      // a click to be certain
      if (subMenuDelay > autoDelay) {
        delay(subMenuDelay - autoDelay);
      }
    }
    // Top-level menus and menu items *must* be clicked on
    Log.debug("Activating menu item " + toString(item));
    click(item);
    waitForIdle();

    // If this item is a menu, make sure its popup is showing before we
    // return
    if (isMenu) {
      JPopupMenu popup = ((javax.swing.JMenu) item).getPopupMenu();
      if (notWaitForComponent(popup, popupDelay)) {
        String msg =
            "Clicking on '"
                + ((javax.swing.JMenu) item).getText()
                + "' never produced a popup menu";
        throw new ComponentMissingException(msg);
      }
      // for OSX 1.4.1; isShowing set before the popup is available
      if (subMenuDelay > autoDelay) {
        delay(subMenuDelay - autoDelay);
      }
    }
  }

  public void selectPopupMenuItem(Component invoker, ComponentLocation loc, String path) {
    Point where = loc.getPoint(invoker);

    if (where.x == -1) {
      where.x = invoker.getWidth() / 2;
    }
    if (where.y == -1) {
      where.y = invoker.getHeight() / 2;
    }
    Component popup = showPopupMenu(invoker, where.x, where.y);
    try {
      Matcher m = new JMenuItemMatcher(path);
      // Don't care about the hierarchy on this one, since there'll only
      // ever be one popup active at a time.
      Component item = BasicFinder.getDefault().find((Container) popup, m);
      selectMenuItem(item);
      waitForIdle();
    } catch (ComponentNotFoundException e) {
      throw new ComponentMissingException("Can't find menu item '" + path + "'");
    } catch (MultipleComponentsFoundException e) {
      throw new ActionFailedException(e.getMessage());
    }
  }

  public Component showPopupMenu(Component invoker) {
    return showPopupMenu(invoker, invoker.getWidth() / 2, invoker.getHeight() / 2);
  }

  public Component showPopupMenu(Component invoker, int x, int y) {
    String where = " at (" + x + "," + y + ")";
    Log.debug("Invoking popup " + where);
    click(invoker, x, y, AWTConstants.POPUP_MASK);

    Component popup = AWT.findActivePopupMenu();
    if (popup == null) {
      String msg =
          "No popup responded to "
              + AWTConstants.POPUP_MODIFIER
              + where
              + " on "
              + toString(invoker);
      throw new ComponentMissingException(msg);
    }
    int POPUP_DELAY = 10000;
    long start = System.currentTimeMillis();
    while (!isReadyForInput(SwingUtilities.getWindowAncestor(popup))
        && System.currentTimeMillis() - start > POPUP_DELAY) {
      sleep();
    }
    return popup;
  }

  public void activate(Window win) {
    // ACTIVATE means a window gets keyboard focus.
    invokeAndWait(win, win::toFront);
    // For pointer-focus systems
    mouseMove(win);
  }

  protected Point getCloseLocation(Container c) {
    Dimension size = c.getSize();
    Insets insets = c.getInsets();
    if (Platform.isOSX()) {
      return new Point(insets.left + 15, insets.top / 2);
    } else {
      return new Point(size.width - insets.right - 10, insets.top / 2);
    }
  }

  public void close(Window w) {
    if (w.isShowing()) {
      // Move to a corner and "pretend" to use the window manager
      // control
      try {
        Point p = getCloseLocation(w);
        mouseMove(w, p.x, p.y);
      } catch (Exception e) {
        // ignore
      }
    }
  }

  protected Point getMoveLocation(Container c) {
    Dimension size = c.getSize();
    Insets insets = c.getInsets();
    return new Point(size.width / 2, insets.top / 2);
  }

  public void move(Container comp, int newx, int newy) {
    Point loc = AWT.getLocationOnScreen(comp);
    moveBy(comp, newx - loc.x, newy - loc.y);
  }

  public void moveBy(Container comp, int dx, int dy) {
    Point loc = AWT.getLocationOnScreen(comp);
    boolean userMovable = userMovable(comp);
    if (userMovable) {
      Point p = getMoveLocation(comp);
      mouseMove(comp, p.x, p.y);
      mouseMove(comp, p.x + dx, p.y + dy);
    }
    invokeAndWait(comp, () -> comp.setLocation(new Point(loc.x + dx, loc.y + dy)));
    if (userMovable) {
      Point p = getMoveLocation(comp);
      mouseMove(comp, p.x, p.y);
    }
  }

  protected Point getResizeLocation(Container c) {
    Dimension size = c.getSize();
    Insets insets = c.getInsets();
    return new Point(size.width - insets.right / 2, size.height - insets.bottom / 2);
  }

  protected boolean userMovable(Component comp) {
    return comp instanceof Dialog || comp instanceof Frame || canMoveWindows();
  }

  protected boolean userResizable(Component comp) {
    if (comp instanceof Dialog) {
      return ((Dialog) comp).isResizable();
    }
    if (comp instanceof Frame) {
      return ((Frame) comp).isResizable();
    }
    // most X11 window managers allow arbitrary resizing
    return canResizeWindows();
  }

  public void resize(Container comp, int width, int height) {
    Dimension size = comp.getSize();
    resizeBy(comp, width - size.width, height - size.height);
  }

  public void resizeBy(Container comp, int dx, int dy) {
    // Fake the pointer motion like we're resizing
    boolean userResizable = userResizable(comp);
    if (userResizable) {
      Point p = getResizeLocation(comp);
      mouseMove(comp, p.x, p.y);
      mouseMove(comp, p.x + dx, p.y + dy);
    }
    invokeAndWait(comp, () -> comp.setSize(comp.getWidth() + dx, comp.getHeight() + dy));
    if (userResizable) {
      Point p = getResizeLocation(comp);
      mouseMove(comp, p.x, p.y);
    }
  }

  protected Point getIconifyLocation(Container c) {
    Dimension size = c.getSize();
    Insets insets = c.getInsets();
    // We know the exact layout of the window manager frames for w32 and
    // OSX. Currently, no way of detecting the WM under X11.  Maybe we
    // could send a WM message (WM_ICONIFY)?
    Point loc = new Point();
    loc.y = insets.top / 2;
    if (Platform.isOSX()) {
      loc.x = 35;
    } else if (Platform.isWindows()) {
      int offset = Platform.isWindowsXP() ? 64 : 45;
      loc.x = size.width - insets.right - offset;
    }
    return loc;
  }

  private static final int MAXIMIZE_BUTTON_OFFSET =
      Platform.isOSX() ? 25 : Platform.isWindows() ? -20 : 0;

  protected Point getMaximizeLocation(Container c) {
    Point loc = getIconifyLocation(c);
    loc.x += MAXIMIZE_BUTTON_OFFSET;
    return loc;
  }

  public void iconify(Frame frame) {
    Point loc = getIconifyLocation(frame);
    if (loc != null) {
      mouseMove(frame, loc.x, loc.y);
    }
    invokeLater(frame, () -> frame.setState(Frame.ICONIFIED));
  }

  public void deiconify(Frame frame) {
    normalize(frame);
  }

  public void normalize(Frame frame) {
    invokeLater(
        frame,
        () -> {
          frame.setState(Frame.NORMAL);
          if (Bugs.hasFrameDeiconifyBug()) {
            frame.setVisible(true);
          }
        });
  }

  public void maximize(Frame frame) {
    Point loc = getMaximizeLocation(frame);
    if (loc != null) {
      mouseMove(frame, loc.x, loc.y);
    }
    invokeLater(
        frame,
        () -> {
          // If maximize is unavailable, set to full-screen size instead.
          try {
            final int MAXIMIZED_BOTH = 6;
            Boolean b =
                (Boolean)
                    Toolkit.class
                        .getMethod("isFrameStateSupported", int.class)
                        .invoke(toolkit, new Object[] {MAXIMIZED_BOTH});
            if (b && !serviceMode) {
              Frame.class.getMethod("setExtendedState", int.class).invoke(frame, MAXIMIZED_BOTH);
            } else {
              throw new RuntimeException("Platform won't maximize");
            }
          } catch (Exception e) {
            Log.debug("Maximize not supported: " + e);
            Rectangle rect = frame.getGraphicsConfiguration().getBounds();
            frame.setLocation(rect.x, rect.y);
            frame.setSize(rect.width, rect.height);
          }
        });
  }

  public void sendEvent(AWTEvent event) {
    // Modifiers are ignored, assuming that an event will be
    // sent that causes modifiers to be sent appropriately.
    if (eventMode == EM_ROBOT) {
      int id = event.getID();
      Log.debug("Sending event id " + id);
      if (id >= MouseEvent.MOUSE_FIRST && id <= MouseEvent.MOUSE_LAST) {
        MouseEvent me = (MouseEvent) event;
        Component comp = me.getComponent();
        if (id == MouseEvent.MOUSE_MOVED) {
          mouseMove(comp, me.getX(), me.getY());
        } else if (id == MouseEvent.MOUSE_DRAGGED) {
          mouseMove(comp, me.getX(), me.getY());
        } else if (id == MouseEvent.MOUSE_PRESSED) {
          mouseMove(comp, me.getX(), me.getY());
          mousePress(me.getModifiersEx() & AWTConstants.BUTTON_DOWN_MASK);
        } else if (id == MouseEvent.MOUSE_ENTERED) {
          mouseMove(comp, me.getX(), me.getY());
        } else if (id == MouseEvent.MOUSE_EXITED) {
          mouseMove(comp, me.getX(), me.getY());
        } else if (id == MouseEvent.MOUSE_RELEASED) {
          mouseMove(comp, me.getX(), me.getY());
          mouseRelease(me.getModifiersEx() & AWTConstants.BUTTON_DOWN_MASK);
        }
      } else if (id >= KeyEvent.KEY_FIRST && id <= KeyEvent.KEY_LAST) {
        KeyEvent ke = (KeyEvent) event;
        if (id == KeyEvent.KEY_PRESSED) {
          keyPress(ke.getKeyCode());
        } else if (id == KeyEvent.KEY_RELEASED) {
          keyRelease(ke.getKeyCode());
        }
      } else {
        Log.warn("Event not supported: " + event);
      }
    } else {
      // Post the event to the appropriate AWT event queue
      postEvent((Component) event.getSource(), event);
    }
  }

  public static String getEventID(AWTEvent event) {
    // Optimize here to avoid field name lookup overhead
    return switch (event.getID()) {
      case MouseEvent.MOUSE_MOVED -> "MOUSE_MOVED";
      case MouseEvent.MOUSE_DRAGGED -> "MOUSE_DRAGGED";
      case MouseEvent.MOUSE_PRESSED -> "MOUSE_PRESSED";
      case MouseEvent.MOUSE_CLICKED -> "MOUSE_CLICKED";
      case MouseEvent.MOUSE_RELEASED -> "MOUSE_RELEASED";
      case MouseEvent.MOUSE_ENTERED -> "MOUSE_ENTERED";
      case MouseEvent.MOUSE_EXITED -> "MOUSE_EXITED";
      case KeyEvent.KEY_PRESSED -> "KEY_PRESSED";
      case KeyEvent.KEY_TYPED -> "KEY_TYPED";
      case KeyEvent.KEY_RELEASED -> "KEY_RELEASED";
      case WindowEvent.WINDOW_OPENED -> "WINDOW_OPENED";
      case WindowEvent.WINDOW_CLOSING -> "WINDOW_CLOSING";
      case WindowEvent.WINDOW_CLOSED -> "WINDOW_CLOSED";
      case WindowEvent.WINDOW_ICONIFIED -> "WINDOW_ICONIFIED";
      case WindowEvent.WINDOW_DEICONIFIED -> "WINDOW_DEICONIFIED";
      case WindowEvent.WINDOW_ACTIVATED -> "WINDOW_ACTIVATED";
      case WindowEvent.WINDOW_DEACTIVATED -> "WINDOW_DEACTIVATED";
      case ComponentEvent.COMPONENT_MOVED -> "COMPONENT_MOVED";
      case ComponentEvent.COMPONENT_RESIZED -> "COMPONENT_RESIZED";
      case ComponentEvent.COMPONENT_SHOWN -> "COMPONENT_SHOWN";
      case ComponentEvent.COMPONENT_HIDDEN -> "COMPONENT_HIDDEN";
      case FocusEvent.FOCUS_GAINED -> "FOCUS_GAINED";
      case FocusEvent.FOCUS_LOST -> "FOCUS_LOST";
      case HierarchyEvent.HIERARCHY_CHANGED -> "HIERARCHY_CHANGED";
      case HierarchyEvent.ANCESTOR_MOVED -> "ANCESTOR_MOVED";
      case HierarchyEvent.ANCESTOR_RESIZED -> "ANCESTOR_RESIZED";
      case PaintEvent.PAINT -> "PAINT";
      case PaintEvent.UPDATE -> "UPDATE";
      case ActionEvent.ACTION_PERFORMED -> "ACTION_PERFORMED";
      case InputMethodEvent.CARET_POSITION_CHANGED -> "CARET_POSITION_CHANGED";
      case InputMethodEvent.INPUT_METHOD_TEXT_CHANGED -> "INPUT_METHOD_TEXT_CHANGED";
      default -> Reflector.getFieldName(event.getClass(), event.getID(), "");
    };
  }

  public static Class<?> getCanonicalClass(Class<?> refClass) {
    // Don't use classnames from anonymous inner classes...
    // Don't use classnames from platform LAF classes...
    String className = refClass.getName();
    while (className.contains("$")
        || className.startsWith("javax.swing.plaf")
        || className.startsWith("com.apple.mrj")) {
      refClass = refClass.getSuperclass();
      className = refClass.getName();
    }
    return refClass;
  }

  public static String toString(Component comp) {
    if (comp == null) {
      return "(null)";
    }

    if (AWT.isTransientPopup(comp)) {
      boolean tooltip = AWT.isToolTip(comp);
      if (AWT.isHeavyweightPopup(comp)) {
        return tooltip
            ? Strings.get("component.heavyweight_tooltip")
            : Strings.get("component.heavyweight_popup");
      } else if (AWT.isLightweightPopup(comp)) {
        return tooltip
            ? Strings.get("component.lightweight_tooltip")
            : Strings.get("component.lightweight_popup");
      }
    } else if (AWT.isSharedInvisibleFrame(comp)) {
      return Strings.get("component.default_frame");
    }
    String name = getDescriptiveName(comp);
    String classDesc = descriptiveClassName(comp.getClass());
    if (name == null) {
      if (AWT.isContentPane(comp)) {
        name = Strings.get("component.content_pane");
      } else if (AWT.isGlassPane(comp)) {
        name = Strings.get("component.glass_pane");
      } else if (comp instanceof JLayeredPane) {
        name = Strings.get("component.layered_pane");
      } else if (comp instanceof JRootPane) {
        name = Strings.get("component.root_pane");
      } else {
        name = classDesc + " instance";
      }
    } else {
      name = "'" + name + "' (" + classDesc + ")";
    }
    return name;
  }

  public static String toString(Object obj) {
    if (obj instanceof Component) {
      return toString((Component) obj);
    } else if (obj instanceof MenuBar) {
      return "MenuBar";
    } else if (obj instanceof MenuItem) {
      return ((MenuItem) obj).getLabel();
    }
    return obj.toString();
  }

  protected static String descriptiveClassName(Class<?> cls) {
    StringBuilder desc = new StringBuilder(simpleClassName(cls));
    Class<?> coreClass = getCanonicalClass(cls);
    String coreClassName = coreClass.getName();
    while (!coreClassName.startsWith("java.awt.") && !coreClassName.startsWith("javax.swing.")) {
      coreClass = coreClass.getSuperclass();
      coreClassName = coreClass.getName();
    }
    if (!coreClass.equals(cls)) {
      desc.append("/");
      desc.append(simpleClassName(coreClass));
    }
    return desc.toString();
  }

  public static String toHierarchyPath(Component c) {
    StringBuilder buf = new StringBuilder();
    Container parent = c.getParent();
    if (parent != null) {
      buf.append(toHierarchyPath(parent));
      buf.append(":");
    }
    buf.append(descriptiveClassName(c.getClass()));
    String name = getDescriptiveName(c);
    if (name != null) {
      buf.append("(");
      buf.append(name);
      buf.append(")");
    } else if (parent != null && parent.getComponentCount() > 1 && c instanceof JPanel) {
      buf.append("[");
      buf.append(getIndex(parent, c));
      buf.append("]");
    }
    return buf.toString();
  }

  public static String toString(AWTEvent event) {
    String name = toString(event.getSource());
    String desc = getEventID(event);
    if (event.getID() == KeyEvent.KEY_PRESSED || event.getID() == KeyEvent.KEY_RELEASED) {
      KeyEvent ke = (KeyEvent) event;
      desc += " (" + AWT.getKeyCode(ke.getKeyCode());
      if (ke.getModifiersEx() != 0) {
        desc += "/" + AWT.getKeyModifiers(ke.getModifiersEx());
      }
      desc += ")";
    } else if (event.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
      desc += " (" + ((InputMethodEvent) event).getCommittedCharacterCount() + ")";
    } else if (event.getID() == KeyEvent.KEY_TYPED) {
      char ch = ((KeyEvent) event).getKeyChar();
      int mods = ((KeyEvent) event).getModifiersEx();
      desc += " ('" + ch + (mods != 0 ? "/" + AWT.getKeyModifiers(mods) : "") + "')";
    } else if (event.getID() >= MouseEvent.MOUSE_FIRST && event.getID() <= MouseEvent.MOUSE_LAST) {
      MouseEvent me = (MouseEvent) event;
      if (me.getModifiersEx() != 0) {
        desc += " <" + AWT.getMouseModifiers(me.getModifiersEx());
        if (me.getClickCount() > 1) {
          desc += "," + me.getClickCount();
        }
        desc += ">";
      }
      desc += " (" + me.getX() + "," + me.getY() + ")";
    } else if (event.getID() == HierarchyEvent.HIERARCHY_CHANGED) {
      HierarchyEvent he = (HierarchyEvent) event;
      long flags = he.getChangeFlags();
      String type = "";
      String bar = "";
      if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
        type += (he.getComponent().isShowing() ? "" : "!") + "SHOWING";
        bar = "|";
      }
      if ((flags & HierarchyEvent.PARENT_CHANGED) != 0) {
        type += bar + "PARENT:" + toString(he.getComponent().getParent());
        bar = "|";
      }
      if ((flags & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
        type += bar + "DISPLAYABILITY";
      }
      desc += " (" + type + ")";
    }
    return desc + " on " + name;
  }

  public static int getEventID(Class<?> cls, String id) {
    return Reflector.getFieldValue(cls, id);
  }

  public static String simpleClassName(Class<?> cls) {
    String name = cls.getName();
    int dot = name.lastIndexOf(".");
    return name.substring(dot + 1);
  }

  public static String getDescriptiveName(Component c) {
    if (AWT.isSharedInvisibleFrame(c)) {
      return Strings.get("component.default_frame");
    }

    String name = getName(c);
    if (name == null) {
      if ((name = getTitle(c)) == null) {
        if ((name = getText(c)) == null) {
          if ((name = getLabel(c)) == null) {
            if ((name = getIconName(c)) == null) {}
          }
        }
      }
    }
    return name;
  }

  private static String getName(Component c) {
    return AWT.hasDefaultName(c) ? null : c.getName();
  }

  private static String getTitle(Component c) {
    if (c instanceof Dialog) {
      return ((Dialog) c).getTitle();
    } else if (c instanceof Frame) {
      return ((Frame) c).getTitle();
    } else if (c instanceof JInternalFrame) {
      return ((JInternalFrame) c).getTitle();
    }
    return null;
  }

  private static String getText(Component c) {
    if (c instanceof AbstractButton) {
      return ComponentTester.stripHTML(((AbstractButton) c).getText());
    } else if (c instanceof JLabel) {
      return ComponentTester.stripHTML(((JLabel) c).getText());
    } else if (c instanceof Label) {
      return ((Label) c).getText();
    }
    return null;
  }

  private static final String LABELED_BY_PROPERTY = "labeledBy";

  private static String getLabel(Component c) {
    String label = null;
    if (c instanceof JComponent) {
      Object obj = ((JComponent) c).getClientProperty(LABELED_BY_PROPERTY);
      // While the default is a JLabel, users may use something else as
      // the property, so be careful.
      if (obj != null) {
        if (obj instanceof JLabel) {
          label = ((JLabel) obj).getText();
        } else if (obj instanceof String) {
          label = (String) obj;
        }
      }
    }
    return ComponentTester.stripHTML(label);
  }

  private static String getIconName(Component c) {
    String icon = null;
    AccessibleContext context = c.getAccessibleContext();
    if (context != null) {
      AccessibleIcon[] icons = context.getAccessibleIcon();
      if (icons != null && icons.length > 0) {
        icon = icons[0].getAccessibleIconDescription();
        if (icon != null) {
          icon = icon.substring(icon.lastIndexOf("/") + 1);
          icon = icon.substring(icon.lastIndexOf("\\") + 1);
        }
      }
    }
    return icon;
  }

  public static int getIndex(Container parent, Component comp) {
    if (comp instanceof Window) {
      Window[] owned = ((Window) parent).getOwnedWindows();
      for (int i = 0; i < owned.length; i++) {
        if (owned[i] == comp) {
          return i;
        }
      }
    } else {
      Component[] children = parent.getComponents();
      for (int i = 0; i < children.length; ++i) {
        if (children[i] == comp) {
          return i;
        }
      }
    }
    return -1;
  }

  private AWTEvent lastEventPosted = null;
  private MouseEvent lastMousePress = null;
  private boolean countingClicks = false;

  protected void postEvent(Component comp, AWTEvent ev) {
    if (Log.isClassDebugEnabled(Robot.class)) {
      Log.debug("POST: " + toString(ev));
    }
    if (eventMode == EM_AWT && AWT.isAWTPopupMenuBlocking()) {
      throw new Error("Event queue is blocked by an active AWT PopupMenu");
    }
    // Force an update of the input state so that we're in synch
    // internally. Otherwise, we might post more events before this
    // one gets processed and end up using stale values for those events.
    state.update(ev);
    EventQueue q = getEventQueue(comp);
    q.postEvent(ev);
    delay(autoDelay);
    AWTEvent prev = lastEventPosted;
    lastEventPosted = ev;
    if (ev instanceof MouseEvent) {
      if (ev.getID() == MouseEvent.MOUSE_PRESSED) {
        lastMousePress = (MouseEvent) ev;
        countingClicks = true;
      } else if (ev.getID() != MouseEvent.MOUSE_RELEASED
          && ev.getID() != MouseEvent.MOUSE_CLICKED) {
        countingClicks = false;
      }
    }

    // Generate a click if there are no events between press/release
    // Unfortunately, I can only guess how the VM generates them
    if (eventMode == EM_AWT
        && ev instanceof MouseEvent me
        && ev.getID() == MouseEvent.MOUSE_RELEASED
        && prev.getID() == MouseEvent.MOUSE_PRESSED) {
      AWTEvent click =
          new MouseEvent(
              comp,
              MouseEvent.MOUSE_CLICKED,
              System.currentTimeMillis(),
              me.getModifiersEx(),
              me.getX(),
              me.getY(),
              me.getClickCount(),
              false);
      postEvent(comp, click);
    }
  }

  /**
   * Wait for the given Condition to return true.  The default timeout may be changed by setting
   * abbot.robot.default_delay.
   *
   * @param condition condition
   * @throws WaitTimedOutError if the default timeout (30s) is exceeded.
   */
  public void wait(Condition condition) {
    wait(condition, defaultDelay);
  }

  /**
   * Wait for the given Condition to return true, waiting for timeout ms.
   *
   * @param condition condition
   * @param timeout   timeout
   * @throws WaitTimedOutError if the timeout is exceeded.
   */
  public void wait(Condition condition, long timeout) {
    wait(condition, timeout, SLEEP_INTERVAL);
  }

  /**
   * Wait for the given Condition to return true, waiting for timeout ms, polling at the given
   * interval.
   *
   * @param condition condition
   * @param timeout   timeout
   * @param interval  interval
   * @throws WaitTimedOutError if the timeout is exceeded.
   */
  public void wait(Condition condition, long timeout, int interval) {
    long start = System.currentTimeMillis();
    while (!condition.test()) {
      if (System.currentTimeMillis() - start > timeout) {
        String msg = "Timed out waiting for " + condition;
        throw new WaitTimedOutError(msg);
      }
      delay(interval);
    }
  }

  public void reset() {
    if (eventMode == EM_ROBOT) {
      Dimension d = toolkit.getScreenSize();
      mouseMove(d.width / 2, d.height / 2);
      mouseMove(d.width / 2 - 1, d.height / 2 - 1);
    } else {
      // clear any held state
      state.clear();
    }
  }

  public Component findFocusOwner() {
    return AWT.getFocusOwner();
  }

  public static boolean canResizeWindows() {
    return !Platform.isWindows() && !Platform.isMacintosh();
  }

  public static boolean canMoveWindows() {
    return !Platform.isWindows() && !Platform.isMacintosh();
  }

  public static int getPreferredRobotAutoDelay() {
    if (Platform.isWindows() || Platform.isOSX() || Platform.isX11()) {
      return 0;
    }
    return 50;
  }

  private static class RobotIdleLock {}
}
