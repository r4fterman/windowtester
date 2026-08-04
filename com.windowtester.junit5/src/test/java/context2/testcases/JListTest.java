/*******************************************************************************
 *  Copyright (c) 2012 Google, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse License v1.0
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
import com.windowtester.runtime.swing.SwingWidgetLocator;
import com.windowtester.runtime.swing.condition.WindowShowingCondition;
import com.windowtester.runtime.swing.locator.JListLocator;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.SwingList;

@ExtendWith(WindowtesterExtension.class)
class JListTest {

  @UIUnderTest(title = "Swing List")
  private SwingList list = new SwingList();

  @Test
  void testRegularClicks(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.wait(new WindowShowingCondition("Swing List"), 1_000);
    IWidgetLocator locator =
        ui.click(
            new JListLocator(
                "/three/two/one",
                new SwingWidgetLocator(
                    JViewport.class,
                    new SwingWidgetLocator(
                        JScrollPane.class, 0, new SwingWidgetLocator(Box.class)))));

    JList<?> jlist = (JList<?>) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValuesList(), new String[] {"/three/two/one"});

    locator =
        ui.click(
            new JListLocator(
                "four",
                new SwingWidgetLocator(
                    JViewport.class,
                    new SwingWidgetLocator(
                        JScrollPane.class, 0, new SwingWidgetLocator(Box.class)))));

    jlist = (JList<?>) ((IWidgetReference) locator).getWidget();
    assertContainsExactly(jlist.getSelectedValuesList(), new String[] {"four"});

    ui.click(
        new JListLocator(
            "two",
            new SwingWidgetLocator(
                JViewport.class,
                new SwingWidgetLocator(JScrollPane.class, 0, new SwingWidgetLocator(Box.class)))));

    assertContainsExactly(jlist.getSelectedValuesList(), new String[] {"two"});

    ui.click(
        new JListLocator(
            "seven",
            new SwingWidgetLocator(
                JViewport.class,
                new SwingWidgetLocator(JScrollPane.class, 0, new SwingWidgetLocator(Box.class)))));
    assertContainsExactly(jlist.getSelectedValuesList(), new String[] {"seven"});
  }

  @Test
  void testCtrlClicks(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("Swing List"), 1_000);
    ui.click(
        new JListLocator(
            "one",
            new SwingWidgetLocator(
                JViewport.class,
                new SwingWidgetLocator(JScrollPane.class, 1, new SwingWidgetLocator(Box.class)))));

    ui.click(
        1,
        new JListLocator(
            "seven",
            new SwingWidgetLocator(
                JViewport.class,
                new SwingWidgetLocator(JScrollPane.class, 1, new SwingWidgetLocator(Box.class)))),
        InputEvent.BUTTON1_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

    IWidgetLocator locator =
        ui.click(
            1,
            new JListLocator(
                "four",
                new SwingWidgetLocator(
                    JViewport.class,
                    new SwingWidgetLocator(
                        JScrollPane.class, 1, new SwingWidgetLocator(Box.class)))),
            InputEvent.BUTTON1_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

    JList<?> jlist = (JList<?>) ((IWidgetReference) locator).getWidget();
    List<?> selectedValuesList = jlist.getSelectedValuesList();

    assertContainsExactly(selectedValuesList, new String[] {"one", "seven", "four"});
  }

  @Test
  void testShiftClicks(@SwingUIContext IUIContext ui) throws WidgetSearchException {
    ui.wait(new WindowShowingCondition("Swing List"), 1_000);
    ui.click(
        new JListLocator(
            "five",
            new SwingWidgetLocator(
                JViewport.class,
                new SwingWidgetLocator(JScrollPane.class, 2, new SwingWidgetLocator(Box.class)))));

    IWidgetLocator locator =
        ui.click(
            1,
            new JListLocator(
                "seven",
                new SwingWidgetLocator(
                    JViewport.class,
                    new SwingWidgetLocator(
                        JScrollPane.class, 2, new SwingWidgetLocator(Box.class)))),
            InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    JList<?> jlist = (JList<?>) ((IWidgetReference) locator).getWidget();
    final List<?> selectedValuesList = jlist.getSelectedValuesList();

    assertContainsExactly(selectedValuesList, new String[] {"five", "six", "seven"});
  }

  private void assertContainsExactly(Collection<?> hosts, Object[] elems) {
    assertContainsExactly(hosts, Arrays.asList(elems));
  }

  private void assertContainsExactly(Collection<?> host, Collection<?> elems) {
    assertTrue(host.containsAll(elems), "Host: " + host + " - Elems: " + elems);
  }
}
