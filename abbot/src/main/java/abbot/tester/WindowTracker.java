package abbot.tester;

import abbot.Log;
import abbot.util.AbbotTimerTask;
import abbot.util.NamedTimer;
import abbot.util.Properties;
import abbot.util.WeakAWTEventListener;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.WeakHashMap;
import javax.swing.SwingUtilities;

/**
 * Keep track of all known root windows, and all known showing/hidden/closed windows.
 */
public class WindowTracker {

  private static class Holder {

    public static final WindowTracker INSTANCE = new WindowTracker();
  }

  /**
   * Maps unique event queues to the set of root windows found on each queue.
   */
  private final Map<EventQueue, Map<Component, Boolean>> contexts;

  /**
   * Maps components to their corresponding event queues.
   */
  private final Map<Component, WeakReference<EventQueue>> queues;

  /**
   * Windows which for which isShowing is true but are not yet ready for input.
   */
  private final Map<Component, Object> pendingWindows = new WeakHashMap<>();

  /**
   * Windows which we deem are ready to use.
   */
  private final Map<Component, Boolean> openWindows = new WeakHashMap<>();

  /**
   * Windows which are not visible.
   */
  private final Map<Component, Boolean> hiddenWindows = new WeakHashMap<>();

  /**
   * Windows which have sent a WINDOW_CLOSE event.
   */
  private final Map<Component, Boolean> closedWindows = new WeakHashMap<>();

  private java.awt.Robot robot;
  private static final int WINDOW_READY_DELAY =
      Properties.getProperty("abbot.window_ready_delay", 1000, 0, 60000);
  private final Timer windowReadyTimer;

  /**
   * Only ever want one of these.
   */
  public static WindowTracker getTracker() {
    return Holder.INSTANCE;
  }

  /**
   * Create an instance of WindowTracker which will track all windows coming and going on the
   * current and later app contexts.
   */
  WindowTracker() {
    ContextTracker contextTracker = new ContextTracker();
    long mask = AWTEvent.WINDOW_EVENT_MASK | AWTEvent.COMPONENT_EVENT_MASK;
    new WeakAWTEventListener(contextTracker, mask);
    WindowReadyTracker windowReadyTracker = new WindowReadyTracker();
    mask = AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.PAINT_EVENT_MASK;
    new WeakAWTEventListener(windowReadyTracker, mask);
    // hold the event queue references weakly
    // each queue maps to a set of components (actually a weak hash map to
    // allow GC of the component keys).
    contexts = new WeakHashMap<>();
    contexts.put(Toolkit.getDefaultToolkit().getSystemEventQueue(), new WeakHashMap<>());
    // hold both the component references and the event queues weakly
    queues = new WeakHashMap<>();
    // Populate stuff that may already have shown/been hidden
    Frame[] frames = Frame.getFrames();
    synchronized (openWindows) {
      for (Frame frame : frames) {
        scanExistingWindows(frame);
      }
    }
    try {
      robot = new java.awt.Robot();
    } catch (AWTException e) {
      // ignore
    }
    windowReadyTimer = new NamedTimer("Window Ready Timer", true);
  }

  private void scanExistingWindows(Window w) {
    // Make sure we catch subsequent show/hide events for this window
    new WindowWatcher(w);
    Window[] windows = w.getOwnedWindows();
    for (Window window : windows) {
      scanExistingWindows(window);
    }
    openWindows.put(w, Boolean.TRUE);
    if (!w.isShowing()) {
      hiddenWindows.put(w, Boolean.TRUE);
    }
    noteContext(w);
  }

  /**
   * Returns whether the window is ready to receive OS-level event input. A window's "isShowing"
   * flag may be set true before the WINDOW_OPENED event is generated, and even after the
   * WINDOW_OPENED is sent the window peer is not guaranteed to be ready.
   */
  public boolean isWindowReady(Window w) {
    synchronized (openWindows) {
      if (openWindows.containsKey(w) && !hiddenWindows.containsKey(w)) {
        return true;
      }
    }
    if (robot != null) {
      checkWindow(w, robot);
    }
    return false;
  }

  /**
   * Explicitly mark the given window as ready for input, bypassing the event-based readiness
   * detection and its {@link #WINDOW_READY_DELAY} fallback timeout.
   * <p>
   * Intended for callers (e.g. test harnesses) that have just shown a window and therefore know it
   * is up: after calling this, {@link #isWindowReady(Window)} returns {@code true} immediately, so
   * the first interaction with the window need not wait out the readiness timeout. Safe to call
   * more than once and independent of window event ordering.
   *
   * @param w the window to mark ready (ignored if {@code null})
   */
  public void setWindowReady(Window w) {
    if (w == null) {
      return;
    }
    synchronized (openWindows) {
      pendingWindows.remove(w);
      closedWindows.remove(w);
      hiddenWindows.remove(w);
      openWindows.put(w, Boolean.TRUE);
    }
  }

  /**
   * Return the event queue corresponding to the given component. In most cases, this is the same as
   * Component.getToolkit().getSystemEventQueue().
   */
  public EventQueue getQueue(Component c) {
    while (c.getParent() != null) {
      c = c.getParent();
    }
    synchronized (contexts) {
      WeakReference<EventQueue> ref = queues.get(c);
      EventQueue q = ref != null ? ref.get() : null;
      if (q == null) {
        q = c.getToolkit().getSystemEventQueue();
      }
      return q;
    }
  }

  /**
   * Returns all known event queues.
   */
  public Collection<EventQueue> getEventQueues() {
    HashSet<EventQueue> set;
    synchronized (contexts) {
      set = new HashSet<>(contexts.keySet());
      for (WeakReference<EventQueue> ref : queues.values()) {
        EventQueue q = ref.get();
        if (q != null) {
          set.add(q);
        }
      }
    }
    return set;
  }

  /**
   * Return all available root Windows.  A root Window is one that has a null parent.  Nominally
   * this means a list similar to that returned by Frame.getFrames().
   */
  public Collection<Component> getRootWindows() {
    Set<Component> set = new HashSet<>();
    // Use Frame.getFrames() here in addition to our watched set, just in
    // case any of them is missing from our set.
    synchronized (contexts) {
      for (Map.Entry<EventQueue, Map<Component, Boolean>> entry : contexts.entrySet()) {
        Map<Component, Boolean> map = entry.getValue();
        set.addAll(map.keySet());
      }
    }
    Frame[] frames = Frame.getFrames();
    Collections.addAll(set, frames);
    return set;
  }

  /**
   * Provides tracking of window visibility state.  We explicitly add this on WINDOW_OPEN and remove
   * it on WINDOW_CLOSE to avoid having to process extraneous ComponentEvents.
   */
  private class WindowWatcher extends WindowAdapter implements ComponentListener {

    public WindowWatcher(Window w) {
      w.addComponentListener(this);
      w.addWindowListener(this);
    }

    public void componentShown(ComponentEvent e) {
      markWindowShowing((Window) e.getSource());
    }

    public void componentHidden(ComponentEvent e) {
      synchronized (openWindows) {
        hiddenWindows.put((Window) e.getSource(), Boolean.TRUE);
        pendingWindows.remove(e.getSource());
      }
    }

    @Override
    public void windowClosed(WindowEvent e) {
      e.getWindow().removeWindowListener(this);
      e.getWindow().removeComponentListener(this);
    }

    public void componentResized(ComponentEvent e) {}

    public void componentMoved(ComponentEvent e) {}
  }

  /**
   * Whenever we get a window on a new event dispatch thread, take note of the thread, since it may
   * correspond to a new event queue and AppContext.
   */
  // FIXME what if it has the same app context? can we check?
  private class ContextTracker implements AWTEventListener {

    public void eventDispatched(AWTEvent ev) {
      ComponentEvent event = (ComponentEvent) ev;
      Component comp = event.getComponent();
      if (!(comp instanceof Window)) {
        return;
      }

      int id = ev.getID();
      if (id == WindowEvent.WINDOW_OPENED) {
        noteOpened(comp);
      } else if (id == WindowEvent.WINDOW_CLOSED) {
        noteClosed(comp);
      } else if (id == WindowEvent.WINDOW_CLOSING) {
        // ignore
      }
      // Take note of all other window events
      else if ((id >= WindowEvent.WINDOW_FIRST && id <= WindowEvent.WINDOW_LAST)
          || id == ComponentEvent.COMPONENT_SHOWN) {
        synchronized (openWindows) {
          if (!getRootWindows().contains(comp) || closedWindows.containsKey(comp)) {
            noteOpened(comp);
          }
        }
      }

      synchronized (contexts) {
        WeakReference<EventQueue> ref = queues.get(comp);
        if (ref != null && !comp.getToolkit().getSystemEventQueue().equals(ref.get())) {
          noteContext(comp);
        }
      }
    }
  }

  private class WindowReadyTracker implements AWTEventListener {

    public void eventDispatched(AWTEvent e) {
      if (e.getID() == MouseEvent.MOUSE_MOVED || e.getID() == MouseEvent.MOUSE_DRAGGED) {
        Component c = (Component) e.getSource();
        Window w = c instanceof Window ? (Window) c : SwingUtilities.getWindowAncestor(c);
        markWindowReady(w);
      }
    }
  }

  private void noteContext(Component comp) {
    EventQueue queue = comp.getToolkit().getSystemEventQueue();
    synchronized (contexts) {
      Map<Component, Boolean> map = contexts.computeIfAbsent(queue, k -> new WeakHashMap<>());
      if (comp instanceof Window && comp.getParent() == null) {
        map.put(comp, Boolean.TRUE);
      }
      queues.put(comp, new WeakReference<>(queue));
    }
  }

  private void noteOpened(Component comp) {
    noteContext(comp);
    // Attempt to ensure the window is ready for input before recognizing
    // it as "open".  There is no Java API for this, so we institute an
    // empirically tested delay.
    if (comp instanceof Window) {
      new WindowWatcher((Window) comp);
      markWindowShowing((Window) comp);
      // Native components don't receive events anyway
      if (comp instanceof FileDialog) {
        markWindowReady((Window) comp);
      }
    }
  }

  private void noteClosed(Component comp) {
    if (comp.getParent() == null) {
      EventQueue queue = comp.getToolkit().getSystemEventQueue();
      synchronized (contexts) {
        Map<Component, Boolean> whm = contexts.get(queue);
        if (whm != null) {
          whm.remove(comp);
        } else {
          EventQueue foundQueue = null;
          for (Map.Entry<EventQueue, Map<Component, Boolean>> entry : contexts.entrySet()) {
            Map<Component, Boolean> map = entry.getValue();
            if (map.containsKey(comp)) {
              foundQueue = entry.getKey();
              map.remove(comp);
            }
          }
          if (foundQueue == null) {
            Log.log(
                "Got WINDOW_CLOSED on "
                    + Robot.toString(comp)
                    + " on a previously unseen context: "
                    + queue
                    + "("
                    + Thread.currentThread()
                    + ")");
          } else {
            Log.log(
                "Window "
                    + Robot.toString(comp)
                    + " sent WINDOW_CLOSED on "
                    + queue
                    + " but sent WINDOW_OPENED on "
                    + foundQueue);
          }
        }
      }
    }
    synchronized (openWindows) {
      openWindows.remove(comp);
      hiddenWindows.remove(comp);
      closedWindows.put(comp, Boolean.TRUE);
      pendingWindows.remove(comp);
    }
  }

  /**
   * Mark the given Window as ready for input.  Indicate whether any pending "mark ready" task
   * should be canceled.
   */
  private void markWindowReady(Window w) {
    synchronized (openWindows) {
      // If the window was closed after the check timer started running,
      // it will have canceled the pending ready.
      // Make sure it's still on the pending list before we actually
      // mark it ready.
      if (pendingWindows.containsKey(w)) {
        closedWindows.remove(w);
        hiddenWindows.remove(w);
        openWindows.put(w, Boolean.TRUE);
        pendingWindows.remove(w);
      }
    }
  }

  /**
   * Indicate a window has set isShowing true and needs to be marked ready when it is actually
   * ready.
   */
  private void markWindowShowing(final Window w) {
    synchronized (openWindows) {
      pendingWindows.put(w, Boolean.TRUE);
    }
  }

  private Insets getInsets(Container c) {
    try {
      Insets insets = c.getInsets();
      if (insets != null) {
        return insets;
      }
    } catch (NullPointerException e) {
      // ignore
    }
    return new Insets(0, 0, 0, 0);
  }

  private static int sign = 1;

  /**
   * Actively check whether the given window is ready for input.
   *
   * @see #isWindowReady
   */
  private void checkWindow(final Window w, java.awt.Robot robot) {
    // Must avoid frame borders, which are insensitive to mouse
    // motion (at least on w32).
    final Insets insets = getInsets(w);
    final int width = w.getWidth();
    final int height = w.getHeight();
    int x = w.getX() + insets.left + (width - (insets.left + insets.right)) / 2;
    int y = w.getY() + insets.top + (height - (insets.top + insets.bottom)) / 2;
    if (x != 0 && y != 0) {
      robot.mouseMove(x, y);
      if (width > height) {
        robot.mouseMove(x + sign, y);
      } else {
        robot.mouseMove(x, y + sign);
      }
      sign = -sign;
    }
    synchronized (openWindows) {
      if (pendingWindows.get(w) == Boolean.TRUE && isEmptyFrame(w)) {
        // Force the frame to be large enough to receive events
        SwingUtilities.invokeLater(
            () -> {
              int nw = Math.max(width, insets.left + insets.right + 3);
              int nh = Math.max(height, insets.top + insets.bottom + 3);
              w.setSize(nw, nh);
            });
      }
      // At worst, time out and say the window is ready
      // after the configurable delay
      AbbotTimerTask task =
          new AbbotTimerTask() {
            public void run() {
              markWindowReady(w);
            }
          };
      windowReadyTimer.schedule(task, WINDOW_READY_DELAY);
      pendingWindows.put(w, task);
    }
  }

  /**
   * We can't get any motion events on an empty frame.
   */
  private boolean isEmptyFrame(Window w) {
    Insets insets = getInsets(w);
    return insets.top + insets.bottom == w.getHeight()
        || insets.left + insets.right == w.getWidth();
  }
}
