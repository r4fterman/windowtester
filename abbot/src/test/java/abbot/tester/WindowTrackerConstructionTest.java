package abbot.tester;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the construction-time race in {@link WindowTracker}.
 *
 * <p>The tracker registers global AWT event listeners in its constructor. Doing so publishes
 * {@code this} to the AWT event dispatch thread, which can invoke
 * {@code ContextTracker.eventDispatched -> getRootWindows()} and access the internal
 * {@code contexts}/{@code queues} maps. If those maps were assigned <em>after</em> the listeners
 * were registered, a window/component event dispatched while the constructor was still running
 * observed {@code contexts == null} and failed on the AWT-EventQueue thread with:
 *
 * <pre>NullPointerException: Cannot enter synchronized block because "this.contexts" is null</pre>
 *
 * <p>This test constructs many trackers while a background thread continuously dispatches window
 * and component events, and asserts that {@link WindowTracker} never throws on the event thread.
 */
class WindowTrackerConstructionTest {

  private static final int CONSTRUCTION_ATTEMPTS = 200;

  @Test
  void constructingTrackerWhileEventsAreDispatchedDoesNotThrow() throws InterruptedException {
    Assumptions.assumeFalse(
        GraphicsEnvironment.isHeadless(), "AWT window events require a display");

    List<Throwable> uncaught = Collections.synchronizedList(new ArrayList<>());
    Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> uncaught.add(throwable));

    // The event source lives outside the construction path so it never interferes with the maps
    // populated from Frame.getFrames() inside the constructor.
    Frame eventSource = new Frame("window-tracker-regression-source");
    AtomicBoolean running = new AtomicBoolean(true);
    Thread eventPump =
        new Thread(
            () -> {
              while (running.get()) {
                // Both event ids route through the getRootWindows() branch of
                // ContextTracker.eventDispatched that used to dereference the null map.
                Toolkit.getDefaultToolkit()
                    .getSystemEventQueue()
                    .postEvent(new WindowEvent(eventSource, WindowEvent.WINDOW_ACTIVATED));
                Toolkit.getDefaultToolkit()
                    .getSystemEventQueue()
                    .postEvent(new ComponentEvent(eventSource, ComponentEvent.COMPONENT_SHOWN));
              }
            },
            "window-event-pump");
    eventPump.setDaemon(true);

    try {
      eventPump.start();
      for (int i = 0; i < CONSTRUCTION_ATTEMPTS; i++) {
        WindowTracker tracker = new WindowTracker();
        assertNotNull(tracker.getRootWindows(), "getRootWindows() must never return null");
      }
      running.set(false);
      eventPump.join(5_000);
    } finally {
      running.set(false);
      eventSource.dispose();
      Thread.setDefaultUncaughtExceptionHandler(previous);
    }

    List<Throwable> trackerFailures =
        uncaught.stream()
            .filter(WindowTrackerConstructionTest::originatesFromWindowTracker)
            .collect(Collectors.toList());
    assertTrue(
        trackerFailures.isEmpty(),
        () -> "WindowTracker threw on the event thread during construction: " + trackerFailures);
  }

  private static boolean originatesFromWindowTracker(Throwable throwable) {
    for (StackTraceElement element : throwable.getStackTrace()) {
      if (WindowTracker.class.getName().equals(element.getClassName())) {
        return true;
      }
    }
    return false;
  }
}
