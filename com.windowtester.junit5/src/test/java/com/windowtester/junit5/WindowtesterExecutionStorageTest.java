package com.windowtester.junit5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the teardown hang in {@link WindowtesterExecutionStorage}.
 *
 * <p>{@code wipe()} used to wait for all windows to disappear in an unbounded {@code while} loop. A
 * window that never became invisible (e.g. a modal dialog left open by a failing test, or a window
 * that would not hide under a headless display server in a Docker container on Jenkins) made that
 * loop spin forever, hanging the test teardown and the whole build.
 */
class WindowtesterExecutionStorageTest {

  @Test
  void awaitDisposalGivesUpWhenAWindowNeverHides() {
    // A window that stays visible forever must not block the teardown indefinitely.
    assertTimeoutPreemptively(
        Duration.ofSeconds(5),
        () ->
            WindowtesterExecutionStorage.awaitDisposal(
                () -> true, // window is always still visible
                () -> {}, // disposing has no effect
                Duration.ofMillis(300),
                Duration.ofMillis(50)));
  }

  @Test
  void awaitDisposalReturnsAsSoonAsWindowsAreGone() {
    // Report "still visible" for the first two polls, then "gone".
    AtomicInteger visibleChecks = new AtomicInteger();
    AtomicInteger disposeCalls = new AtomicInteger();

    assertTimeoutPreemptively(
        Duration.ofSeconds(5),
        () ->
            WindowtesterExecutionStorage.awaitDisposal(
                () -> visibleChecks.getAndIncrement() < 2,
                disposeCalls::incrementAndGet,
                Duration.ofSeconds(10), // large timeout; must not be reached
                Duration.ofMillis(10)));

    assertEquals(3, visibleChecks.get(), "should stop polling once no window is visible");
    assertTrue(disposeCalls.get() >= 1, "should keep re-disposing while windows remain");
  }
}
