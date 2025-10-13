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
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swing.locator.JMenuItemLocator;
import com.windowtester.runtime.swing.locator.JTableItemLocator;
import java.awt.Point;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SimpleTable;

@ExtendWith(WindowtesterExtension.class)
class JTableTest2 {

  @UIUnderTest private SimpleTable panel = new SimpleTable();

  @Test
  void testDoubleClicks(@SwingUIContext IUIContext ui) throws Exception {
    IWidgetReference locator =
        (IWidgetReference) ui.click(2, new JTableItemLocator(new Point(1, 0)));

    JTable table = (JTable) locator.getWidget();
    TableModel model = table.getModel();

    ui.enterText("next\n");

    ui.click(new JTableItemLocator(new Point(1, 0)));
    assertEquals(model.getValueAt(table.getSelectedRow(), table.getSelectedColumn()), "next");
  }

  @Test
  void testContextMenuSelection(@SwingUIContext IUIContext ui) throws Exception {
    ui.click(new JTableItemLocator(new Point(2, 1)));
    ui.contextClick(new JTableItemLocator(new Point(2, 1)), new JMenuItemLocator("choice3"));

    ui.contextClick(new JTableItemLocator(new Point(0, 2)), new JMenuItemLocator("choice1"));
  }
}
