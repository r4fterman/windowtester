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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.windowtester.junit5.SwingUIContext;
import com.windowtester.junit5.UIUnderTest;
import com.windowtester.junit5.WindowtesterExtension;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JListLocator;
import com.windowtester.runtime.swing.locator.NamedWidgetLocator;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.JList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SwingList;

@ExtendWith(WindowtesterExtension.class)
class NamedListTest {

  @UIUnderTest(title = "Swing List Demo")
  private SwingList panel = new SwingList();

  @Test
  void testNamedLists(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.wait(new WindowShowingCondition("Swing List Demo"), 1_000);

    // named locator
    IWidgetLocator locator = ui.click(new JListLocator("one", new NamedWidgetLocator("list1")));
    JList jlist = (JList) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValues(), new String[] {"one"});

    locator = ui.click(new JListLocator("four", new NamedWidgetLocator("list2")));
    jlist = (JList) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValues(), new String[] {"four"});

    locator = ui.click(new JListLocator("seven", new NamedWidgetLocator("list3")));
    jlist = (JList) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValues(), new String[] {"seven"});

    locator = ui.click(new JListLocator("five", new NamedWidgetLocator("list1")));
    jlist = (JList) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValues(), new String[] {"five"});
  }

  ////////////////////////////////////////////////////////////////////////
  //
  // Assertion helpers
  //
  ////////////////////////////////////////////////////////////////////////

  private void assertContainsExactly(Collection<?> host, Collection<?> elems) {
    assertTrue(host.containsAll(elems));
  }

  private void assertContainsExactly(Object[] hosts, Object[] elems) {
    assertContainsExactly(Arrays.asList(hosts), Arrays.asList(elems));
  }
}
