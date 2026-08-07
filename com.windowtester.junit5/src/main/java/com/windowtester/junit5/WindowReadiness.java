package com.windowtester.junit5;

import abbot.tester.WindowTracker;
import java.awt.Window;

/**
 * Waits for a freshly shown window to become able to receive OS level input.
 * <p>
 * A window reports {@code isShowing() == true} before its peer accepts synthetic events, which is
 * why {@link WindowTracker} probes for readiness instead of trusting that flag. Skipping the probe
 * and declaring the window ready lets the first interaction of a test fire into that gap, where it
 * is silently dropped: the widget stays visible, enabled and correctly located while its state
 * never changes, which reads as a flaky test rather than as a harness bug.
 * <p>
 * Separated from {@link WindowtesterExtension} so the handshake can be verified without depending
 * on an operating system level race.
 */
final class WindowReadiness {

  /** Answers whether a window is ready, and can be told to stop asking. */
  interface Probe {

    boolean isReady(Window window);

    /**
     * Declare the window ready without having observed it. Only a last resort: the answer is a
     * guess, and a wrong guess costs the next interaction.
     */
    void forceReady(Window window);
  }

  private static final Probe WINDOW_TRACKER =
      new Probe() {
        @Override
        public boolean isReady(Window window) {
          return WindowTracker.getTracker().isWindowReady(window);
        }

        @Override
        public void forceReady(Window window) {
          WindowTracker.getTracker().setWindowReady(window);
        }
      };

  private WindowReadiness() {
    // static only
  }

  static Probe windowTracker() {
    return WINDOW_TRACKER;
  }

  /**
   * Polls {@code probe} until the window is ready.
   * <p>
   * Cheap whenever readiness can be observed — the tracker answers within a few tens of
   * milliseconds once the window is the foreground window. Where it cannot be observed at all, for
   * instance on a display without a window manager, this costs {@code abbot.window_ready_delay}
   * (1s by default), because that is when the tracker's own fallback declares the window ready.
   * <p>
   * Reaching {@code timeoutMillis} means readiness is never going to be reported. The window is
   * then declared ready anyway: a lost first click is a better outcome than a suite that hangs on a
   * window nobody can measure.
   *
   * @return {@code true} if readiness was observed, {@code false} if it had to be forced
   */
  static boolean awaitReadyForInput(
      Window window, Probe probe, long timeoutMillis, long pollMillis) {
    var deadline = System.currentTimeMillis() + timeoutMillis;

    while (!probe.isReady(window)) {
      if (System.currentTimeMillis() >= deadline) {
        probe.forceReady(window);
        return false;
      }
      sleep(pollMillis);
    }
    return true;
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
