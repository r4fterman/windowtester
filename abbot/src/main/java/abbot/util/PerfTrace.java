package abbot.util;

/**
 * Lightweight, opt-in timing instrumentation used to diagnose where test playback spends its time
 * (e.g. {@code Robot.waitForIdle()} stalls vs. locator/component-tree traversal).
 * <p>
 * Enable it for a test run with system properties:
 * <pre>
 *   -Dwt.perf.trace=true                 (turn tracing on; off by default, zero overhead)
 *   -Dwt.perf.trace.threshold=50         (only report spans that take at least N ms; default 50)
 * </pre>
 * Reported spans are written to {@code System.err} with the prefix {@code [WT-PERF]} so they can be
 * grepped and summed:
 * <pre>
 *   [WT-PERF]  9998ms  Robot.waitForIdle
 *   [WT-PERF]    12ms  SwingWidgetFinder.findAll ... -&gt; 1 match(es)
 * </pre>
 * This class is intentionally free of any WindowTester/abbot dependencies beyond plain JDK calls so
 * it can be used from any module and adds no cost when disabled.
 */
public final class PerfTrace {

  private static final boolean ENABLED = Boolean.getBoolean("wt.perf.trace");
  private static final long THRESHOLD_MS = Long.getLong("wt.perf.trace.threshold", 50L);

  private PerfTrace() {
    // no instances
  }

  /**
   * @return {@code true} if performance tracing is enabled for this run.
   */
  public static boolean isEnabled() {
    return ENABLED;
  }

  /**
   * Start timing a span. Returns a start timestamp (nanos) to be passed to {@link #end(long,
   * String)}. Returns {@code 0} and does nothing measurable when tracing is disabled.
   */
  public static long start() {
    return ENABLED ? System.nanoTime() : 0L;
  }

  /**
   * Finish timing a span started with {@link #start()} and report it if it took at least the
   * configured threshold. No-op when tracing is disabled.
   *
   * @param startNanos the value returned by {@link #start()}
   * @param label      a short description of the span (kept cheap to build by callers)
   */
  public static void end(long startNanos, String label) {
    if (!ENABLED) {
      return;
    }
    long ms = (System.nanoTime() - startNanos) / 1_000_000L;
    if (ms >= THRESHOLD_MS) {
      System.err.printf("[WT-PERF] %6dms  %s%n", ms, label);
    }
  }
}
