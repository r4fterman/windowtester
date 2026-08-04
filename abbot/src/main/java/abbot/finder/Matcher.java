package abbot.finder;

import java.awt.Component;

/**
 * Provides an indication whether a Component matches some desired criteria. For use with
 * implementations of {@link ComponentFinder}. You can conveniently inline a custom matcher like
 * so:<br>
 * <pre>
 * ComponentFinder finder;
 * ...
 * // Find a label with known text
 * JLabel label = (JLabel)finder.find(new Matcher() {
 * public boolean matches(Component c) {
 * return c instanceof JLabel
 * &amp;&amp; "expected text".equals(((JLabel)c).getText());
 * }
 * });
 * </pre>
 *
 * @see ComponentFinder
 */
public interface Matcher {

  /**
   * Return whether the given Component matches some lookup criteria.
   *
   * @param component component
   * @return true if matches
   */
  boolean matches(Component component);

  /**
   * Reset any traversal-scoped state before a fresh search.
   * <p>
   * Most matchers are stateless and use the default no-op. Order-dependent matchers (e.g. index
   * matchers that count occurrences during a single tree traversal) must reset their counters here
   * and propagate the reset to any delegate matchers, so that repeated searches with the same
   * matcher instance start from a clean state.
   */
  default void reset() {
    // no-op by default; stateful matchers override
  }
}
