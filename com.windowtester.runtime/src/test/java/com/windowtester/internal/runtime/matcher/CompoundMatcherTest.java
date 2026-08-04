package com.windowtester.internal.runtime.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.windowtester.runtime.locator.IWidgetMatcher;
import org.junit.jupiter.api.Test;

class CompoundMatcherTest {

  @Test
  void matches_only_when_both_matchers_match() {
    assertTrue(new CompoundMatcher(matching(true), matching(true)).matches(new Object()));
    assertFalse(new CompoundMatcher(matching(true), matching(false)).matches(new Object()));
    assertFalse(new CompoundMatcher(matching(false), matching(true)).matches(new Object()));
    assertFalse(new CompoundMatcher(matching(false), matching(false)).matches(new Object()));
  }

  @Test
  void does_not_evaluate_second_matcher_when_first_fails() {
    var first = matching(false);
    var second = matching(true);

    boolean result = new CompoundMatcher(first, second).matches(new Object());

    assertFalse(result);
    assertEquals(1, first.calls, "first matcher should be evaluated");
    assertEquals(0, second.calls, "second matcher should be short-circuited");
  }

  @Test
  void reset_propagates_to_both_matchers() {
    var first = matching(true);
    var second = matching(true);

    new CompoundMatcher(first, second).reset();

    assertTrue(first.wasReset);
    assertTrue(second.wasReset);
  }

  private static CountingMatcher matching(boolean result) {
    return new CountingMatcher(result);
  }

  private static final class CountingMatcher implements IWidgetMatcher<Object> {

    private final boolean result;
    private int calls;
    private boolean wasReset;

    private CountingMatcher(boolean result) {
      this.result = result;
    }

    @Override
    public boolean matches(Object widget) {
      calls++;
      return result;
    }

    @Override
    public void reset() {
      wasReset = true;
    }
  }
}
