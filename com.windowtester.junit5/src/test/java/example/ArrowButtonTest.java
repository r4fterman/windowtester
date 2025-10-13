package example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import abbot.tester.ComponentTester;
import com.windowtester.junit5.UIUnderTest;
import com.windowtester.junit5.WindowtesterExtension;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Source code for Tutorial 1. Simple unit tests for example.ArrowButton. Also demonstrates the use
 * of ComponentTestFixture.
 */
@ExtendWith(WindowtesterExtension.class)
class ArrowButtonTest {

  private ComponentTester tester;
  private String gotClick;
  private ArrowButton left;
  private ArrowButton right;
  private ArrowButton up;
  private ArrowButton down;

  @UIUnderTest private JPanel panel = createPanel();

  @BeforeEach
  protected void setUp() {
    tester = ComponentTester.getTester(ArrowButton.class);
  }

  @Test
  void testClickLeft() {
    gotClick = null;
    tester.actionClick(left);

    assertEquals(ArrowButton.LEFT, gotClick, () -> "Action click LEFT failed");
  }

  @Test
  void testClickRight() {
    gotClick = null;
    tester.actionClick(right);

    assertEquals(ArrowButton.RIGHT, gotClick, () -> "Action click RIGHT failed");
  }

  @Test
  void testClickUp() {
    gotClick = null;
    tester.actionClick(up);

    assertEquals(ArrowButton.UP, gotClick, () -> "Action click UP failed");
  }

  @Test
  void testClickDown() {
    gotClick = null;
    tester.actionClick(down);

    assertEquals(ArrowButton.DOWN, gotClick, () -> "Action click DOWN failed");
  }

  private JPanel createPanel() {
    this.left = new ArrowButton(ArrowButton.LEFT);
    left.addActionListener(ev -> gotClick = ev.getActionCommand());

    this.right = new ArrowButton(ArrowButton.RIGHT);
    right.addActionListener(ev -> gotClick = ev.getActionCommand());

    this.up = new ArrowButton(ArrowButton.UP);
    up.addActionListener(ev -> gotClick = ev.getActionCommand());

    this.down = new ArrowButton(ArrowButton.DOWN);
    down.addActionListener(ev -> gotClick = ev.getActionCommand());

    JPanel pane = new JPanel(new FlowLayout(FlowLayout.LEFT));
    pane.add(right);
    pane.add(up);
    pane.add(down);
    pane.add(left);
    return pane;
  }
}
