package example;

import abbot.finder.matchers.ClassMatcher;
import abbot.finder.matchers.JMenuItemMatcher;
import abbot.tester.JButtonTester;
import abbot.tester.JTextComponentTester;
import java.awt.*;
import javax.swing.*;
import junit.extensions.abbot.ComponentTestFixture;

/**
 * Demonstrates testing a simple Swing UI ({@link CelsiusConverter}) with Abbot.
 *
 * @author Satadip Dutta (original)
 * @author Tom Roche (ported to new-style Abbot, added more tests)
 * @version $Id: CelsiusConverterTest.java,v 1.1 2006-11-03 18:52:42 pq Exp $
 */
public class CelsiusConverterTest extends ComponentTestFixture {

  // for precision testing
  static final int DEFAULT_PRECISION = 0;
  static final int PRECISION2 = 2;
  static final int PRECISION3 = 3;

  private CelsiusConverter cc;
  private JTextField tempCelsius;
  private JButton convertButton;
  private JLabel outputLabel;

  private JTextComponentTester tt;
  private JButtonTester bt;

  /**
   * For older versions of JUnit.
   */
  public CelsiusConverterTest(final String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    cc = new CelsiusConverter();
    final JFrame frame = new JFrame();
    cc.enframe(frame);
    // Display at the current frame's desired size (avoids packing)
    showWindow(frame, null, false);

    // only one JTextField in our UI, so we can just class-match
    tempCelsius = (JTextField) getFinder().find(new ClassMatcher(JTextField.class));
    // ditto for the JButton
    convertButton = (JButton) getFinder().find(new ClassMatcher(JButton.class));
    // But there's 2 JLabel's in our UI, so we need to add more
    // information
    outputLabel =
        (JLabel)
            getFinder()
                .find(
                    new ClassMatcher(JLabel.class) {
                      @Override
                      public boolean matches(final Component c) {
                        final String text = CelsiusConverter.lookupString("output.label.text");
                        return super.matches(c) && ((JLabel) c).getText().equals(text);
                      }
                    });

    tt = new JTextComponentTester();
    bt = new JButtonTester();
  }

  public void testNegativeNumber() throws Exception {

    tt.actionEnterText(tempCelsius, "-45"); // $NON-NLS-1$
    bt.actionClick(convertButton);
    assertEquals(CelsiusConverter.fahrenheitOutput(-49, DEFAULT_PRECISION), outputLabel.getText());
  }

  public void testBadInput() throws Exception {
    // get default text
    final String originalText = outputLabel.getText();
    // nothing should change if the input is not parseable as a double
    tt.actionEnterText(tempCelsius, " HELLO "); // $NON-NLS-1$
    bt.actionClick(convertButton);
    assertTrue(
        "Output changed for bad input", outputLabel.getText().equals(originalText)); // $NON-NLS-1$
  }

  public void testChangePrecision() throws Exception {
    final JMenuItem item2 =
        (JMenuItem)
            getFinder().find(new JMenuItemMatcher(String.valueOf(PRECISION2))); // $NON-NLS-1$

    // the output should update to reflect a higher precision after making
    // a change, even with no new input

    // initial precision is 0
    tt.actionEnterText(tempCelsius, "25.23"); // $NON-NLS-1$
    bt.actionClick(convertButton);

    // now update precision and make sure output fields update too
    tt.actionSelectMenuItem(item2);

    assertEquals(
        "Failed to reflect change in precision", //$NON-NLS-1$
        CelsiusConverter.fahrenheitOutput(77.41, PRECISION2),
        outputLabel.getText());
  }

  public void testHighPrecision() throws Exception {
    final JMenuItem item3 =
        (JMenuItem)
            getFinder().find(new JMenuItemMatcher(String.valueOf(PRECISION3))); // $NON-NLS-1$
    tt.actionSelectMenuItem(item3);
    tt.actionEnterText(tempCelsius, "-45.543"); // $NON-NLS-1$
    bt.actionClick(convertButton);

    assertEquals(
        "Failed to show answer with proper precision", //$NON-NLS-1$
        CelsiusConverter.fahrenheitOutput(-49.977, PRECISION3),
        outputLabel.getText());
  }
}
