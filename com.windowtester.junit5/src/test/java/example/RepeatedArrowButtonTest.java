package example;

import static org.junit.jupiter.api.Assertions.assertTrue;

import abbot.tester.ComponentTester;
import com.windowtester.junit5.UIUnderTest;
import com.windowtester.junit5.WindowtesterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Source code for Tutorial 1. Simple unit tests for example.ArrowButton. Also demonstrates the use
 * of ComponentTestFixture.
 */
@ExtendWith(WindowtesterExtension.class)
class RepeatedArrowButtonTest {

  private ComponentTester tester;

  @UIUnderTest private final ArrowButton arrow = new ArrowButton(ArrowButton.LEFT);

  private int count = 0;

  @BeforeEach
  protected void setUp() {
    tester = ComponentTester.getTester(ArrowButton.class);
  }

  @Test
  void testRepeatedFire() {
    arrow.addActionListener(ev -> ++count);

    // Hold the button down for 5 seconds
    tester.mousePress(arrow);
    tester.actionDelay(5000);
    tester.mouseRelease();

    assertTrue(count > 1, () -> "Didn't get any repeated events");
  }
}
