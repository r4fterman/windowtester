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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.windowtester.junit5.SwingUIContext;
import com.windowtester.junit5.UIUnderTest;
import com.windowtester.junit5.WindowtesterExtension;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.condition.IsVisibleCondition;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JComboBoxLocator;
import com.windowtester.runtime.swing.locator.NamedWidgetLocator;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.ComboBoxes;

@ExtendWith(WindowtesterExtension.class)
class JComboBoxTest {

  @UIUnderTest(title = "Swing Combo Boxes")
  private ComboBoxes panel = new ComboBoxes();

  @Test
  void testComboClicks(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Swing Combo Boxes"), 1_000);

    IWidgetLocator locator =
        ui.click(
            new JComboBoxLocator(
                "Cat",
                new SwingWidgetLocator(Box.class, 0, new SwingWidgetLocator(ComboBoxes.class))));
    JComboBox<String> cBox = (JComboBox<String>) ((IWidgetReference) locator).getWidget();

    assertEquals("Cat", cBox.getSelectedItem());

    JComboBoxLocator comboBoxLocator =
        new JComboBoxLocator("Rabbit", new NamedWidgetLocator("pets"));

    ui.assertThat(comboBoxLocator.isEnabled());

    ui.click(comboBoxLocator);

    assertEquals("Rabbit", cBox.getSelectedItem());

    locator =
        ui.click(
            new JComboBoxLocator(
                "yellow",
                new com.windowtester.runtime.swing.SwingWidgetLocator(
                    javax.swing.Box.class,
                    1,
                    new com.windowtester.runtime.swing.SwingWidgetLocator(ComboBoxes.class))));
    cBox = (JComboBox) ((IWidgetReference) locator).getWidget();
    assertEquals("yellow", cBox.getSelectedItem());
  }

  @Test
  void testComboEnterTextFails(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Swing Combo Boxes"), 1_000);

    SwingWidgetLocator petsComboBoxLocator = new SwingWidgetLocator(JComboBox.class, "pets");
    ui.wait(new IsVisibleCondition(petsComboBoxLocator), 1_000);

    ui.click(2, petsComboBoxLocator);
    ui.enterText("pink\n");

    ui.pause(300);

    SwingWidgetLocator colorsComboBoxLocator = new SwingWidgetLocator(JComboBox.class, "colors");
    ui.wait(new IsVisibleCondition(colorsComboBoxLocator), 1_000);

    IWidgetLocator locator = ui.click(colorsComboBoxLocator);
    JComboBox<String> cBox = (JComboBox<String>) ((IWidgetReference) locator).getWidget();
    ((JTextField) cBox.getEditor().getEditorComponent()).setText("");

    ui.enterText("pink\n");

    assertEquals("pink", cBox.getSelectedItem());
  }
}
