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

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class ComboBoxes extends JPanel {

  private final JComboBox<String> petList;
  private final JComboBox<String> colorsList;

  public ComboBoxes() {
    Box box = Box.createVerticalBox();
    String[] petStrings = {"Bird", "Cat", "Dog", "Rabbit", "Pig"};

    // Create the combo box, select the item at index 4.
    // Indices start at 0, so 4 specifies the pig.
    petList = new JComboBox<>(petStrings);
    petList.setSelectedIndex(4);
    petList.setName("pets");
    box.add(petList, BorderLayout.CENTER);

    String[] colors = {"red", "blue", "yellow", "white", "black"};
    colorsList = new JComboBox<>(colors);
    colorsList.setEditable(true);
    colorsList.setName("colors");
    Box box2 = Box.createHorizontalBox();
    box2.add(colorsList, BorderLayout.CENTER);
    add(box, BorderLayout.NORTH);
    add(box2, BorderLayout.SOUTH);
  }

  public JComboBox<String> getComboBox1() {
    return petList;
  }

  public JComboBox<String> getComboBox2() {
    return colorsList;
  }
}
