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

import com.windowtester.junit5.SwingUIContext;
import com.windowtester.junit5.UIUnderTest;
import com.windowtester.junit5.WindowtesterExtension;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JTreeItemLocator;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SwingTree;

@ExtendWith(WindowtesterExtension.class)
class SwingTreeTest {

  @UIUnderTest(title = "Swing Tree Demo")
  private SwingTree panel = new SwingTree("Swing Tree Demo");

  @Test
  void testMain(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Swing Tree Demo"), 1_000);

    JTreeItemLocator treeItemLocator =
        new JTreeItemLocator(
            "Root/Parent4",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane1")));
    ui.click(2, treeItemLocator);
    ui.assertThat(treeItemLocator.isSelected());
    ui.click(
        2,
        new JTreeItemLocator(
            "Root/Parent4/Child41",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane1"))));

    JTreeItemLocator locator2 =
        new JTreeItemLocator(
            "Root/Parent4/Child41/grandChild411",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane1")));
    ui.click(locator2);
    ui.assertThat(locator2.isSelected());

    ui.click(
        new JTreeItemLocator(
            "Root/Item 4",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane2"))));
    ui.click(
        2,
        new JTreeItemLocator(
            "Root/Item 0",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane2"))));

    JTreeItemLocator locator3 =
        new JTreeItemLocator(
            "Root/Item 0/Node 01",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane2")));
    ui.click(locator3);
    ui.assertThat(locator3.isSelected());
  }
}
