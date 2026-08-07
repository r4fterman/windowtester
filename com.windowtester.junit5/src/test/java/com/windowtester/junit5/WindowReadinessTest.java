package com.windowtester.junit5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Locks the readiness handshake that {@link WindowtesterExtension} performs before handing a window
 * to a test.
 * <p>
 * The regression this guards against is a harness that declares a freshly shown window ready
 * instead of waiting for it to become ready. That shortcut lets the first interaction of a test
 * fire before the window's peer accepts events, and the interaction is dropped. It is a race, so it
 * only shows up sometimes and downstream looks like a flaky application test rather than a harness
 * bug.
 * <p>
 * The race itself cannot be provoked reliably in a unit test — it depends on how long the operating
 * system takes to realize a window. What can be pinned down is the behaviour whose absence caused
 * it: this must poll until the window reports ready, and must not short-circuit that by forcing it.
 */
class WindowReadinessTest {

  private static final long TIMEOUT_MILLIS = 5_000;
  private static final long POLL_MILLIS = 1;

  private final Window window = null;

  @Test
  void waitsUntilTheWindowReportsReady() {
    var probe = new RecordingProbe(3);

    var observed = WindowReadiness.awaitReadyForInput(window, probe, TIMEOUT_MILLIS, POLL_MILLIS);

    assertTrue(observed, "Readiness was observed, so it must not be reported as forced");
    assertEquals(
        4,
        probe.readyQueries,
        "Expected the wait to keep asking until the window reported ready. Asking once and moving"
            + " on is the regression: the first interaction then fires before the window can"
            + " accept events and is silently dropped.");
    assertEquals(
        0,
        probe.forcedReady,
        "Readiness was observable, so it must never be forced. Forcing it is what swallows the"
            + " first interaction.");
  }

  @Test
  void forcesReadinessOnlyAfterGivingUp() {
    var probe = new RecordingProbe(Integer.MAX_VALUE);

    var observed =
        assertTimeoutPreemptively(
            Duration.ofSeconds(5),
            () -> WindowReadiness.awaitReadyForInput(window, probe, 50, POLL_MILLIS),
            "A window that never reports ready must not block the suite");

    assertFalse(observed, "Readiness was never observed, so it must be reported as forced");
    assertEquals(
        1, probe.forcedReady, "Expected exactly one last-resort attempt to unblock the suite");
  }

  /** Reports ready once it has been asked {@code queriesUntilReady} times. */
  private static final class RecordingProbe implements WindowReadiness.Probe {

    private final int queriesUntilReady;

    private int readyQueries;
    private int forcedReady;

    private RecordingProbe(int queriesUntilReady) {
      this.queriesUntilReady = queriesUntilReady;
    }

    @Override
    public boolean isReady(Window window) {
      return ++readyQueries > queriesUntilReady;
    }

    @Override
    public void forceReady(Window window) {
      forcedReady++;
    }
  }
}
