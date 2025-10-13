package example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import abbot.finder.BasicFinder;
import abbot.finder.ComponentFinder;
import abbot.finder.TestHierarchy;
import abbot.finder.matchers.ClassMatcher;
import abbot.tester.JListLocation;
import abbot.tester.JListTester;
import com.windowtester.junit5.UIUnderTest;
import com.windowtester.junit5.WindowtesterExtension;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Source test code for Tutorial 2, test fixture for the LabeledList.
 */
@ExtendWith(WindowtesterExtension.class)
class LabeledListTest {

  @UIUnderTest private LabeledList labeledList;
  private ComponentFinder finder;

  @BeforeEach
  void setUp() {
    finder = new BasicFinder(new TestHierarchy());

    String[] contents = {"one", "two", "three"};
    this.labeledList = new LabeledList(contents);
  }

  @Test
  void testLabelChangedOnSelectionChange() throws Throwable {
    // The interface abbot.finder.Matcher allows you to define whatever
    // matching specification you'd like.  We know there's only one
    // JList in the hierarchy we're searching, so we can look up by
    // class with an instance of ClassMatcher.
    Component list = finder.find(new ClassMatcher(JList.class));
    assertInstanceOf(JList.class, list);

    // We could also use an instance of ClassMatcher, but this shows
    // how you can put more conditions into the Matcher.
    JLabel label =
        (JLabel)
            finder.find(
                labeledList,
                c -> c.getClass().equals(JLabel.class) && c.getParent() == labeledList);

    JListTester tester = new JListTester();

    // Select by row index or by value
    tester.actionSelectRow((JList<String>) list, new JListLocation(1));
    assertEquals("Selected: two", label.getText(), () -> "Wrong label after selection");

    tester.actionSelectRow((JList<String>) list, new JListLocation(2));
    assertEquals("Selected: three", label.getText(), () -> "Wrong label after selection");

    tester.actionSelectRow((JList<String>) list, new JListLocation(0));
    assertEquals("Selected: one", label.getText(), () -> "Wrong label after selection");
  }
}
