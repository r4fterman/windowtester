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

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

public class SwingList extends JPanel {

  public SwingList() {
    super(new FlowLayout());

    Box box = Box.createHorizontalBox();
    box.add(new JScrollPane(createList1()));
    box.add(Box.createRigidArea(new Dimension(15, 0)));
    box.add(new JScrollPane(createList2()));
    box.add(Box.createRigidArea(new Dimension(15, 0)));
    box.add(new JScrollPane(createList3()));

    add(box);
  }

  private JList<String> createList1() {
    JList<String> list = new JList<>(createListModel());
    list.setName("list1");
    list.setSelectedIndex(0);
    list.setVisibleRowCount(15);
    return list;
  }

  private JList<String> createList2() {
    JList<String> list = new JList<>(createListModel());
    list.setName("list2");
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.setSelectedIndex(0);
    list.setVisibleRowCount(15);
    return list;
  }

  private JList<String> createList3() {
    JList<String> list = new JList<>(createListModel());
    list.setName("list3");
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.setSelectedIndex(0);
    list.setVisibleRowCount(15);
    return list;
  }

  private ListModel<String> createListModel() {
    DefaultListModel<String> listModel = new DefaultListModel<>();
    listModel.addElement("one");
    listModel.addElement("two");
    listModel.addElement("/three/two/one");
    listModel.addElement("four");
    listModel.addElement("five");
    listModel.addElement("six");
    listModel.addElement("seven");
    return listModel;
  }
}
