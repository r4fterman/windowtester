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
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JTableItemLocator;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import javax.swing.JTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SimpleTable;

@ExtendWith(WindowtesterExtension.class)
class JTableTest {

  @UIUnderTest(title = "TableDemo")
  private SimpleTable panel;

  @BeforeEach
  void setUp() {
    panel = new SimpleTable();
  }

  @AfterEach
  void tearDown() {
    panel.setVisible(false);
    panel = null;
  }

  @Test
  void selectConsecutiveTableRows(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.wait(new WindowShowingCondition("TableDemo"), 1_000);

    var table00 = selectTableItem(new Point(0, 0), ui);
    assertEquals(1, table00.getSelectedRowCount());

    var table20 =
        selectTableItem(
            new Point(2, 0), InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, ui);
    assertEquals(3, table20.getSelectedRowCount());

    ui.assertThat(new JTableItemLocator(new Point(2, 0)).isSelected());
    ui.assertThat(new JTableItemLocator(new Point(1, 0)).isSelected());
    ui.assertThat(new JTableItemLocator(new Point(0, 0)).isSelected());
  }

  @Test
  void testCtrlClicks(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    var table = selectTableItem(new Point(0, 0), ui);
    assertEquals(1, table.getSelectedRowCount());

    selectTableItem(
        new Point(2, 0),
        InputEvent.BUTTON1_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(),
        ui);
    assertEquals(2, table.getSelectedRowCount());

    ui.assertThat(new JTableItemLocator(new Point(2, 0)).isSelected());
    ui.assertThat(new JTableItemLocator(new Point(1, 0)).isSelected(false));
    ui.assertThat(new JTableItemLocator(new Point(0, 0)).isSelected());
  }

  @Test
  void testTableRowClicks(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    var table = selectTableItem(new Point(0, 1), ui);

    var model = table.getModel();
    assertEquals("two", model.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));

    ui.click(new JTableItemLocator(new Point(1, 3)));
    assertEquals("eight", model.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));

    ui.click(new JTableItemLocator(new Point(5, 1)));
    assertEquals("twenty-two", model.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));

    ui.click(new JTableItemLocator(new Point(0, 2)));
    assertEquals("three", model.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));
  }

  private JTable selectTableItem(Point point, IUIContext ui) throws WidgetSearchException {
    var tableItemLocator = new JTableItemLocator(point);
    var locator = ui.click(tableItemLocator);

    ui.assertThat(tableItemLocator.isSelected());

    return (JTable) ((IWidgetReference) locator).getWidget();
  }

  private JTable selectTableItem(Point point, int modifierMask, IUIContext ui)
      throws WidgetSearchException {
    var tableItemLocator = new JTableItemLocator(point);
    var locator = ui.click(1, tableItemLocator, modifierMask);

    ui.assertThat(tableItemLocator.isSelected());

    return (JTable) ((IWidgetReference) locator).getWidget();
  }
}
