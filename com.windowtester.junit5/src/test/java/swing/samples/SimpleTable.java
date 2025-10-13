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
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class SimpleTable extends JPanel {

  private final JTable table;
  private final JPopupMenu popup;
  private boolean choice1;
  private boolean choice2;

  class PopupTrigger extends MouseAdapter {

    @Override
    public void mouseReleased(MouseEvent e) {
      if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {

        int x = e.getX();
        int y = e.getY();

        popup.show(table, x, y);
        // m_clickedPath = path;
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {

        int x = e.getX();
        int y = e.getY();

        popup.show(table, x, y);
        // m_clickedPath = path;
      }
    }
  }

  private static class DataModel extends AbstractTableModel {

    private Object[][] data = {
      {"one", "two", "three", "four"},
      {"five", "six", "seven", "eight"},
      {"nine", "ten", "one", "twelve"},
      {"thirteen", "fourteen", "fifteen", "sixteen"},
      {"seventeen", "eighteen", "ninteen", "twenty"},
      {"twenty-one", "twenty-two", "twenty-three", "twenty-four"}
    };

    public DataModel() {
      // addTableModelListener(new TML());
    }

    @Override
    public int getColumnCount() {
      return data[0].length;
    }

    @Override
    public int getRowCount() {
      return data.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
      return data[row][col];
    }

    @Override
    public void setValueAt(Object val, int row, int col) {
      data[row][col] = val;
      // Indicate the change has happened:
      fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
      return true;
    }
  }

  public SimpleTable() {
    setLayout(new GridLayout(1, 0));

    //		 add popop menu
    popup = new JPopupMenu();
    JMenuItem menuItem1 = new JMenuItem("choice1");
    menuItem1.addActionListener(e -> choice1 = !choice1);
    popup.add(menuItem1);

    JMenuItem menuItem2 = new JMenuItem("choice2");
    menuItem1.addActionListener(e -> choice2 = !choice2);
    popup.add(menuItem2);

    JMenu menu = new JMenu("submenu");
    JMenuItem menuItem3 = new JMenuItem("choice3");
    menu.add(menuItem3);
    popup.add(menu);

    table = new JTable(new DataModel());
    table.setPreferredScrollableViewportSize(new Dimension(500, 70));
    table.addMouseListener(new PopupTrigger());

    // Create the scroll pane and add the table to it.
    JScrollPane scrollPane = new JScrollPane(table);

    // Add the scroll pane to this panel.
    add(scrollPane);
  }
}
