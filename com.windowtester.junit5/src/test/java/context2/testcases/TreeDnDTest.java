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
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.swing.locator.JTreeItemLocator;
import com.windowtester.runtime.swing.locator.NamedWidgetLocator;
import javax.swing.JTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.TreeDnD;

@ExtendWith(WindowtesterExtension.class)
class TreeDnDTest {

  @UIUnderTest private TreeDnD panel = new TreeDnD();

  @Test
  void testTreeDnD(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.click(
        new JTreeItemLocator("JTree/colors/blue", new NamedWidgetLocator(JTree.class, "tree1")));
    ui.dragTo(new JTreeItemLocator("JTree/sports", new NamedWidgetLocator(JTree.class, "tree2")));

    ui.click(new JTreeItemLocator("JTree/food/hot dogs", new NamedWidgetLocator("tree2")));
    ui.dragTo(new JTreeItemLocator("JTree/colors", new NamedWidgetLocator("tree1")));
    ui.pause(10000);
  }
}
