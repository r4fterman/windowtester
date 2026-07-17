package com.windowtester.junit5;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Window;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

public class WindowtesterExecutionStorage {

  private static final Logger LOGGER =
      System.getLogger(WindowtesterExecutionStorage.class.getName());

  private static final String UI_COMPONENT_KEY = "UI_COMPONENT";
  private static final String UI_CONTEXT_KEY = "UI_CONTEXT";

  /**
   * Upper bound for how long {@link #wipe()} waits for all windows to disappear before giving up, so
   * a window that never hides cannot hang the test teardown (and the build) forever.
   */
  private static final Duration DISPOSAL_TIMEOUT = Duration.ofSeconds(10);

  private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

  private final Store store;

  public WindowtesterExecutionStorage(ExtensionContext extensionContext) {
    this.store = extensionContext.getStore(Namespace.create("Windowtester", "UI"));
  }

  public Component saveUIComponent(Component uiComponent) {
    store.put(UI_COMPONENT_KEY, uiComponent);
    return uiComponent;
  }

  public Object saveUIContext(Object uiContext) {
    store.put(UI_CONTEXT_KEY, uiContext);
    return uiContext;
  }

  void wipe() {
    wipeUIComponent();
    forceCloseAllWindows();
    awaitDisposal(
        WindowtesterExecutionStorage::anyWindowStillVisible,
        WindowtesterExecutionStorage::forceCloseAllWindows,
        DISPOSAL_TIMEOUT,
        POLL_INTERVAL);
    logLeftoverWindows();
  }

  /**
   * Wait until no window is visible any more, re-closing on every poll, but never block longer than
   * {@code timeout}. A window that refuses to hide (e.g. a modal dialog left open by a failing test,
   * or a window that will not disappear under a headless display server) must not hang this teardown
   * and, with it, the whole build.
   */
  static void awaitDisposal(
      BooleanSupplier anyWindowStillVisible,
      Runnable closeAllWindows,
      Duration timeout,
      Duration pollInterval) {
    long deadlineNanos = System.nanoTime() + timeout.toNanos();
    while (anyWindowStillVisible.getAsBoolean()) {
      if (System.nanoTime() - deadlineNanos >= 0) {
        return;
      }
      try {
        TimeUnit.MILLISECONDS.sleep(pollInterval.toMillis());
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
      closeAllWindows.run();
    }
  }

  /**
   * Actively hide and dispose every window on the event dispatch thread. Doing this on the EDT (and
   * hiding before disposing) closes windows more reliably than a bare {@link Window#dispose()} call,
   * including modal dialogs whose nested event loop still pumps the {@code invokeAndWait} task.
   */
  private static void forceCloseAllWindows() {
    runOnEventDispatchThread(
        () -> {
          for (Window window : Window.getWindows()) {
            window.setVisible(false);
            window.dispose();
          }
        });
  }

  private static void runOnEventDispatchThread(Runnable action) {
    if (EventQueue.isDispatchThread()) {
      action.run();
      return;
    }
    try {
      EventQueue.invokeAndWait(action);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } catch (InvocationTargetException ex) {
      LOGGER.log(Level.WARNING, "Failed to close windows on the event dispatch thread", ex);
    }
  }

  // Check if any window is still active
  private static boolean anyWindowStillVisible() {
    return Arrays.stream(Window.getWindows()).anyMatch(Window::isVisible);
  }

  /**
   * Report any window that survived the teardown so a leftover window that could poison a subsequent
   * test does not go unnoticed (rather than being closed silently, which is impossible here, or
   * hanging the build, which is what this whole path avoids).
   */
  private static void logLeftoverWindows() {
    var leftovers =
        Arrays.stream(Window.getWindows()).filter(Window::isVisible).collect(Collectors.toList());
    if (leftovers.isEmpty()) {
      return;
    }
    var descriptions =
        leftovers.stream()
            .map(WindowtesterExecutionStorage::describe)
            .collect(Collectors.joining(", "));
    LOGGER.log(
        Level.WARNING,
        "{0} window(s) still visible after teardown timeout of {1}; "
            + "they may affect subsequent tests: {2}",
        leftovers.size(),
        DISPOSAL_TIMEOUT,
        descriptions);
  }

  private static String describe(Window window) {
    var type = window.getClass().getName();
    String title = null;
    if (window instanceof Frame frame) {
      title = frame.getTitle();
    } else if (window instanceof Dialog dialog) {
      title = dialog.getTitle();
    }
    return title == null || title.isBlank() ? type : type + "[" + title + "]";
  }

  private void wipeUIComponent() {
    Object o = store.get(UI_COMPONENT_KEY);
    if (o instanceof Dialog dialog) {
      dialog.setVisible(false);
      dialog.dispose();
    } else if (o instanceof Window window) {
      window.setVisible(false);
      window.dispose();
    } else if (o instanceof Component component) {
      component.setVisible(false);
    }
    store.remove(UI_COMPONENT_KEY);
    store.remove(UI_CONTEXT_KEY);
  }
}
