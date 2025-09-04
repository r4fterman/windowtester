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
import com.windowtester.runtime.swing.locator.JButtonLocator;
import com.windowtester.runtime.swing.locator.NamedWidgetLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import swing.samples.TextInputDemo;

@ExtendWith(WindowtesterExtension.class)
class NamedWidgetLocatorTest {

  @UIUnderTest(title = "TextInputDemo")
  private TextInputDemo panel = new TextInputDemo();

  @Test
  void testTextEntry(@SwingUIContext IUIContext ui) throws Exception {
    ui.wait(new WindowShowingCondition("TextInputDemo"));
    ui.enterText("337 Bull Mountain Rd");
    NamedWidgetLocator namedWidgetLocator = new NamedWidgetLocator("city");
    ui.click(namedWidgetLocator);
    ui.enterText("Tigard");
    ui.assertThat(namedWidgetLocator.hasText("Tigard"));

    // TODO [author=Dan] Apparently, spinners do not have a default name on Windows
    // but appear to have one on Linux so maybe this is JDK dependent.
    // commenting out for now for further research
    //		ui.click(new NamedWidgetLocator("Spinner.nextButton"));
    //		ui.click(new NamedWidgetLocator("Spinner.nextButton"));

    ui.click(new JButtonLocator("Set address"));
    ui.click(new NamedWidgetLocator("clear"));
  }
}
