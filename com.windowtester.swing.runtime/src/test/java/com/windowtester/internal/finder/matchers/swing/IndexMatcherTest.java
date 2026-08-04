package com.windowtester.internal.finder.matchers.swing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import abbot.finder.Matcher;
import java.awt.Component;
import javax.swing.JButton;
import org.junit.jupiter.api.Test;

class IndexMatcherTest {

  @Test
  void matches_only_the_component_at_the_configured_index() {
    var matcher = new IndexMatcher(alwaysMatch(), 1);

    assertFalse(matcher.matches(new JButton()), "0th match is not the target index");
    assertTrue(matcher.matches(new JButton()), "1st match is the target index");
    assertFalse(matcher.matches(new JButton()), "2nd match is past the target index");
  }

  @Test
  void reset_restarts_counting_so_a_repeated_search_matches_again() {
    var matcher = new IndexMatcher(alwaysMatch(), 1);

    // first traversal reaches the target index
    assertFalse(matcher.matches(new JButton()));
    assertTrue(matcher.matches(new JButton()));

    // without reset the counter keeps growing and would never hit index 1 again
    assertFalse(matcher.matches(new JButton()));

    // after reset the next traversal counts from zero again
    matcher.reset();
    assertFalse(matcher.matches(new JButton()));
    assertTrue(matcher.matches(new JButton()));
  }

  @Test
  void reset_propagates_to_the_delegate_matcher() {
    var delegate = new ResettableMatcher();

    new IndexMatcher(delegate, 0).reset();

    assertTrue(delegate.wasReset);
  }

  private static Matcher alwaysMatch() {
    return component -> true;
  }

  private static final class ResettableMatcher implements Matcher {

    private boolean wasReset;

    @Override
    public boolean matches(Component component) {
      return true;
    }

    @Override
    public void reset() {
      wasReset = true;
    }
  }
}
