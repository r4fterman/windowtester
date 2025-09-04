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
package swing.samples;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

public class SwingButton extends JPanel {

  private boolean buttonClicked;
  private boolean checkboxClicked;
  private boolean radioButtonClicked;
  private boolean toggleButtonClicked;

  /**
   * Create the panel
   */
  public SwingButton() {
    super();

    JButton button = new JButton();
    button.addActionListener(
        e -> {
          System.out.println("button clicked");
          buttonClicked = true;
        });

    button.setText("Test Button");
    add(button);

    JCheckBox checkbox = new JCheckBox("CheckBox", false);
    checkbox.addActionListener(
        e -> {
          System.out.println("checkbox clicked");
          checkboxClicked = true;
        });

    add(checkbox);

    JRadioButton radioButton = new JRadioButton("RadioButton", false);
    radioButton.addActionListener(
        e -> {
          System.out.println("radio button clicked");
          radioButtonClicked = true;
        });

    add(radioButton);

    JToggleButton toggleButton = new JToggleButton("ToggleButton", false);
    toggleButton.addActionListener(
        e -> {
          System.out.println("toggle button clicked");
          toggleButtonClicked = true;
        });

    add(toggleButton);
  }

  private boolean getButtonClicked() {
    return buttonClicked;
  }

  private boolean getCheckboxClicked() {
    return checkboxClicked;
  }

  private boolean getRadioButtonClicked() {
    return radioButtonClicked;
  }

  private boolean getToggleButtonClicked() {
    return toggleButtonClicked;
  }
}
