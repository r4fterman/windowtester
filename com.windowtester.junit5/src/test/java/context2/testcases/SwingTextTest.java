/*******************************************************************************
 *  Copyright (c) 2012 Google, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Google, Inc. - initial API and implementation
 *******************************************************************************/
package context2.testcases;

import static org.junit.jupiter.api.Assertions.fail;

import abbot.tester.ActionFailedException;
import com.windowtester.junit5.SwingUIContext;
import com.windowtester.junit5.UIUnderTest;
import com.windowtester.junit5.WindowtesterExtension;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JMenuItemLocator;
import com.windowtester.runtime.swing.locator.JTextComponentLocator;
import com.windowtester.runtime.swing.locator.LabeledTextLocator;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SwingText;

@ExtendWith(WindowtesterExtension.class)
class SwingTextTest {

  @UIUnderTest(title = "Swing Text")
  private SwingText panel = new SwingText();

  @Test
  void testTextEntry(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Swing Text"));

    ui.pause(200);
    ui.click(new SwingWidgetLocator(JTextField.class, "textField"));
    ui.enterText("foo\b\n");

    ui.click(new LabeledTextLocator("Name"));
    ui.enterText(" Smith");
    ui.assertThat(new LabeledTextLocator("Name").hasText("Jane Smith"));

    // context clicks
    ui.contextClick(
        new JTextComponentLocator(JTextField.class, 0, null), new JMenuItemLocator("choice2"));

    ui.contextClick(
        new JTextComponentLocator(JTextField.class, 0, null), new JMenuItemLocator("choice1"));

    try {
      ui.contextClick(
          new JTextComponentLocator(JTextField.class, 0, null), new JMenuItemLocator("bogus"));
      fail("should have thrown a CNF exception");
    } catch (ActionFailedException e) {
      fail("should not have thrown " + e);
    } catch (WidgetSearchException e) {
      // pass
    }
  }
}
