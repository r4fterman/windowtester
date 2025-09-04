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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.windowtester.junit5.SwingUIContext;
import com.windowtester.junit5.UIUnderTest;
import com.windowtester.junit5.WindowtesterExtension;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JMenuItemLocator;
import com.windowtester.runtime.swing.locator.JTreeItemLocator;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import javax.swing.JTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SwingTree;

@ExtendWith(WindowtesterExtension.class)
class JTreeTest {

  @UIUnderTest private SwingTree panel;

  @BeforeEach
  void setUp() {
    panel = new SwingTree("Swing Tree Example");
  }

  @Test
  void testTreeSelections(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.wait(new WindowShowingCondition("Swing Tree Example"), 1_000);

    var firstClickTree =
        doTreeClick(
            1, "Root/Parent1/Child10/grandChild102", "tree1", InputEvent.BUTTON1_DOWN_MASK, ui);

    assertEquals(1, firstClickTree.getSelectionRows().length);
    assertEquals(
        "grandChild102", firstClickTree.getSelectionPath().getLastPathComponent().toString());

    var secondClickTree = doTreeClick(1, "Root/Parent3/Child30", "tree1", ui);

    assertEquals(1, secondClickTree.getSelectionRows().length);
    assertEquals("Child30", secondClickTree.getSelectionPath().getLastPathComponent().toString());
  }

  @Test
  void testTreeShiftSelections(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Swing Tree Example"), 1_000);

    var firstClickTree = doTreeClick(1, "Root/Item 0/Node 01", "tree2", ui);

    doTreeClick(
        1,
        "Root/Item 1/Node 10",
        "tree2",
        InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
        ui);

    assertEquals(3, firstClickTree.getSelectionPaths().length);

    var tree = doTreeClick(1, "Root/Item 0/Node 01", "tree2", ui);

    doTreeClick(
        1,
        "Root/Item 1/Node 10",
        "tree2",
        InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
        ui);

    assertEquals(3, tree.getSelectionPaths().length);
  }

  @Test
  void testTreeCtrlSelections(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.wait(new WindowShowingCondition("Swing Tree Example"), 1_000);

    // test ctrl clicks
    doTreeClick(1, "Root/Parent1/Child10/grandChild100", "tree1", InputEvent.BUTTON1_DOWN_MASK, ui);

    var secondClickTree =
        doTreeClick(
            1,
            "Root/Parent1/Child10/grandChild102",
            "tree1",
            InputEvent.BUTTON1_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(),
            ui);

    assertEquals(2, secondClickTree.getSelectionPaths().length);
  }

  @Test
  void testContextMenuSelection(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.wait(new WindowShowingCondition("Swing Tree Example"), 1_000);

    assertFalse(panel.isChoice1());
    ui.contextClick(
        new JTreeItemLocator("Root/Item 1/Node 11", "tree2"), new JMenuItemLocator("choice1"));

    assertTrue(panel.isChoice1());

    assertFalse(panel.isChoice2());
    ui.contextClick(
        new JTreeItemLocator("Root/Item 2/Node 21", "tree2"), new JMenuItemLocator("choice2"));
    assertTrue(panel.isChoice2());
  }

  @Test
  void testDoubleClicks(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.wait(new WindowShowingCondition("Swing Tree Example"), 1_000);

    var tree = doTreeClick(2, "Root/Parent2", "tree1", ui);

    assertTrue(tree.isExpanded(tree.getSelectionPath()));
  }

  private JTree doTreeClick(int clickCount, String node, String treeName, IUIContext ui)
      throws WidgetSearchException {
    final var treeItemLocator = createTreeItemLocator(node, treeName);

    var locator = ui.click(clickCount, treeItemLocator);
    return (JTree) ((IWidgetReference) locator).getWidget();
  }

  private JTree doTreeClick(int clickCount, String node, String treeName, int mask, IUIContext ui)
      throws WidgetSearchException {
    var treeItemLocator = createTreeItemLocator(node, treeName);
    var locator = ui.click(clickCount, treeItemLocator, mask);
    return (JTree) ((IWidgetReference) locator).getWidget();
  }

  private JTreeItemLocator createTreeItemLocator(String node, String treeName) {
    return new JTreeItemLocator(node, treeName);
  }
}
