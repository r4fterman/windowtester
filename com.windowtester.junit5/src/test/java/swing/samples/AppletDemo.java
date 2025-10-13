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

// : c14:List.java
// <applet code=List width=250 height=375></applet>
// From 'Thinking in Java, 3rd ed.' (c) Bruce Eckel 2002
// www.BruceEckel.com. See copyright notice in CopyRight.txt.

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionListener;

public class AppletDemo extends JApplet {

  private final String[] flavors = {
    "Chocolate",
    "Strawberry",
    "Vanilla Fudge Swirl",
    "Mint Chip",
    "Mocha Almond Fudge",
    "Rum Raisin",
    "Praline Cream",
    "Mud Pie"
  };

  private final DefaultListModel<String> lItems = new DefaultListModel<>();
  private final JList<String> lst = new JList<>(lItems);
  private final JTextArea t = new JTextArea(flavors.length, 20);
  private final JButton b = new JButton("Add Item");

  private int count = 0;

  private final ActionListener bl =
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (count < flavors.length) {
            lItems.add(0, flavors[count++]);
          } else {
            // Disable, since there are no more
            // flavors left to be added to the List
            b.setEnabled(false);
          }
        }
      };

  private final ListSelectionListener ll =
      e -> {
        if (e.getValueIsAdjusting()) {
          return;
        }
        t.setText("");

        List<String> items = lst.getSelectedValuesList();
        for (String item : items) {
          t.append(item + "\n");
        }
      };

  public void init() {
    Container cp = getContentPane();
    t.setEditable(false);
    cp.setLayout(new FlowLayout());
    // Create Borders for components:
    Border brd = BorderFactory.createMatteBorder(1, 1, 2, 2, Color.BLACK);
    lst.setBorder(brd);
    t.setBorder(brd);
    // Add the first four items to the List
    for (int i = 0; i < 4; i++) {
      lItems.addElement(flavors[count++]);
    }
    // Add items to the Content Pane for Display
    cp.add(t);
    cp.add(lst);
    cp.add(b);
    // Register event listeners
    lst.addListSelectionListener(ll);
    b.addActionListener(bl);
  }

  public static void main(String[] args) {
    run(new AppletDemo(), 250, 375);
  }

  public static void run(JApplet applet, int width, int height) {
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(applet);
    frame.setSize(width, height);
    applet.init();
    applet.start();
    frame.setVisible(true);
  }
}
