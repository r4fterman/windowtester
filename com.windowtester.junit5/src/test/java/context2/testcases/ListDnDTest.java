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
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JListLocator;
import com.windowtester.runtime.swing.locator.NamedWidgetLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.DragListDemo;

@ExtendWith(WindowtesterExtension.class)
class ListDnDTest {

  @UIUnderTest(title = "Drag List Demo")
  private DragListDemo panel = new DragListDemo();

  @Test
  void testListDnD(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Drag List Demo"), 1_000);

    ui.click(new JListLocator("1 (list 1)", new NamedWidgetLocator("list1")));
    ui.dragTo(new JListLocator("1 (list 2)", new NamedWidgetLocator("list2")));
    ui.pause(1_000);
  }
}
