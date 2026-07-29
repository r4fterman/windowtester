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
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JListLocator;
import com.windowtester.runtime.swing.locator.NamedWidgetLocator;
import java.util.Collection;
import java.util.List;
import javax.swing.JList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SwingList;

@ExtendWith(WindowtesterExtension.class)
class NamedListTest {

  @UIUnderTest(title = "Swing List Demo")
  private SwingList panel = new SwingList();

  @Test
  void panel_with_list1_should_contain_element_one(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Swing List Demo"), 1_000);
    ui.pause(200);

    IWidgetLocator locator = ui.click(new JListLocator("one", new NamedWidgetLocator("list1")));
    JList<String> jlist = (JList<String>) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValuesList(), List.of("one"));
  }

  @Test
  void panel_with_list2_should_contain_element_four(@SwingUIContext IUIContext ui)
      throws Exception {
    ui.wait(new WindowShowingCondition("Swing List Demo"), 1_000);
    ui.pause(200);

    IWidgetLocator locator = ui.click(new JListLocator("four", new NamedWidgetLocator("list2")));
    JList<String> jlist = (JList<String>) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValuesList(), List.of("four"));
  }

  @Test
  void panel_with_list3_should_contain_element_seven(@SwingUIContext IUIContext ui)
      throws Exception {
    ui.wait(new WindowShowingCondition("Swing List Demo"), 1_000);
    ui.pause(200);

    IWidgetLocator locator = ui.click(new JListLocator("seven", new NamedWidgetLocator("list3")));
    JList<String> jlist = (JList<String>) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValuesList(), List.of("seven"));
  }

  @Test
  void panel_with_list1_should_contain_element_five(@SwingUIContext IUIContext ui)
      throws Exception {
    ui.wait(new WindowShowingCondition("Swing List Demo"), 1_000);
    ui.pause(200);

    IWidgetLocator locator = ui.click(new JListLocator("five", new NamedWidgetLocator("list1")));
    JList<String> jlist = (JList<String>) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValuesList(), List.of("five"));
  }

  private void assertContainsExactly(Collection<String> host, Collection<String> elems) {
    assertTrue(host.containsAll(elems));
  }
}
