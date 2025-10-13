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
import com.windowtester.runtime.swing.condition.WindowDisposedCondition;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JTreeItemLocator;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SwingTree;

@ExtendWith(WindowtesterExtension.class)
class SwingTreeTest2 {

  @UIUnderTest private SwingTree panel = new SwingTree("Swing Tree Demo");

  @Test
  void testMain(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Swing Tree Demo"), 1_000);

    JTreeItemLocator treeItemLocator =
        new JTreeItemLocator(
            "Root/Item 2",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane2")));
    ui.click(2, treeItemLocator, InputEvent.BUTTON1_DOWN_MASK);

    JTreeItemLocator treeItemLocator2 =
        new JTreeItemLocator(
            "Root/Item 3",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane2")));
    ui.click(1, treeItemLocator2, InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    ui.assertThat(treeItemLocator2.isSelected());
    ui.assertThat(treeItemLocator.isSelected());

    ui.click(
        2,
        new JTreeItemLocator(
            "Root/Parent1",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane1"))),
        InputEvent.BUTTON1_DOWN_MASK);

    JTreeItemLocator treeItemLocator3 =
        new JTreeItemLocator(
            "Root/Parent1/Child10",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane1")));
    ui.assertThat(treeItemLocator3.isVisible());
    ui.click(2, treeItemLocator3, InputEvent.BUTTON1_DOWN_MASK);

    JTreeItemLocator treeItemLocator4 =
        new JTreeItemLocator(
            "Root/Parent1/Child10/grandChild100",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane1")));
    ui.assertThat(treeItemLocator4.isVisible());
    ui.click(treeItemLocator4);
    ui.click(
        1,
        new JTreeItemLocator(
            "Root/Parent1/Child10/grandChild102",
            new SwingWidgetLocator(
                JViewport.class, new SwingWidgetLocator(JScrollPane.class, "scrollPane1"))),
        InputEvent.BUTTON1_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    ui.wait(new WindowDisposedCondition("Swing Tree Example"));
  }
}
