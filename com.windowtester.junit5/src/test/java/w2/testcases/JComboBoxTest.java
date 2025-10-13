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
package w2.testcases;

import static com.windowtester.internal.runtime.junit.core.ExecutionMonitor.getUI;

import com.windowtester.runtime.IUIContext;
import javax.swing.JComboBox;

public class JComboBoxTest
//    extends UITestCaseSwing
{

  JComboBox cBox;

  private IUIContext ui;

  public JComboBoxTest() {
    //  super(ComboBoxes.class);
  }

  protected void setUp() throws Exception {
    ui = getUI();
  }

  public void testComboClicks() throws Exception {

    //		IWidgetLocator locator;
    //		ui.wait(new WindowShowingCondition("Swing Combo Boxes"));
    //		locator = ui.click(new JComboBoxLocator("Cat",new
    // com.windowtester.runtime.swing.SwingWidgetLocator(javax.swing.Box.class, 0, new
    // com.windowtester.runtime.swing.SwingWidgetLocator(ComboBoxes.class))));
    //		cBox = (JComboBox)((IWidgetReference)locator).getWidget();
    //		assertEquals("Cat",cBox.getSelectedItem());
    //		JComboBoxLocator comboBoxLocator = new JComboBoxLocator("Rabbit",new
    // NamedWidgetLocator("pets"));
    //		ui.assertThat(comboBoxLocator.isEnabled());
    //	//	ui.ensureThat(comboBoxLocator.hasFocus());
    //		ui.click(comboBoxLocator);
    //		assertEquals("Rabbit",cBox.getSelectedItem());
    //
    //		locator = ui.click(new JComboBoxLocator("yellow",new
    // com.windowtester.runtime.swing.SwingWidgetLocator(javax.swing.Box.class, 1, new
    // com.windowtester.runtime.swing.SwingWidgetLocator(ComboBoxes.class))));
    //		cBox = (JComboBox)((IWidgetReference)locator).getWidget();
    //		assertEquals("yellow",cBox.getSelectedItem());

  }

  /*	public void testComboEnterTextFails() throws ComponentNotFoundException, MultipleComponentsFoundException, WidgetSearchException {

  	IWidgetLocator locator;
  	ui.click(2,new JComboBoxLocator(new com.windowtester.runtime.swing.SwingWidgetLocator(javax.swing.Box.class, 1, new com.windowtester.runtime.swing.SwingWidgetLocator(ComboBoxes.class))));
  	ui.enterText("pink\n");
  	ui.pause(300);
  	locator = ui.click(new JComboBoxLocator("pink",new com.windowtester.runtime.swing.SwingWidgetLocator(javax.swing.Box.class, 1, new com.windowtester.runtime.swing.SwingWidgetLocator(ComboBoxes.class))));
  	cBox = (JComboBox)((IWidgetReference)locator).getWidget();
  	assertEquals("pink",cBox.getSelectedItem());

  }

  */

}
