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

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

public class SwingMenus extends JFrame {

  private Component selectedMenuItem;

  public SwingMenus(String title) {
    super(title);

    ActionListener actionListener = e -> selectedMenuItem = (Component) e.getSource();

    MouseAdapter mouseAdapter =
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            selectedMenuItem = (Component) e.getSource();
          }
        };

    //	Create the menu bar.
    JMenuBar menuBar = new JMenuBar();

    // Build the first menu.
    JMenu parentMenu = new JMenu("parent");
    menuBar.add(parentMenu);
    // a group of JMenuItems
    JMenuItem childMenuItem = new JMenuItem("child 1");
    childMenuItem.addActionListener(actionListener);
    parentMenu.add(childMenuItem);
    JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem("child 2");
    rbMenuItem.addActionListener(actionListener);
    rbMenuItem.setEnabled(false);
    parentMenu.add(rbMenuItem);
    JCheckBoxMenuItem cbMenuItem = new JCheckBoxMenuItem("child 3");
    cbMenuItem.addActionListener(actionListener);
    parentMenu.add(cbMenuItem);

    JMenu submenu = new JMenu("submenu");
    JMenuItem grandchildMenuItem = new JMenuItem("grandchild");
    grandchildMenuItem.addActionListener(actionListener);
    submenu.add(grandchildMenuItem);
    parentMenu.add(submenu);

    // Build second menu in the menu bar.
    JMenu topMenu = new JMenu("top");
    topMenu.addMouseListener(mouseAdapter);
    menuBar.add(topMenu);
    JMenu submenu1 = new JMenu("submenu1");
    topMenu.add(submenu1);
    JMenu submenu2 = new JMenu("submenu2");
    submenu1.add(submenu2);
    JMenuItem item1 = new JMenuItem("item1");
    JMenuItem item2 = new JMenuItem("item2");
    submenu2.add(item1);
    submenu2.add(item2);
    setJMenuBar(menuBar);
  }
}
