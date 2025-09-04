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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class SwingTree extends JFrame {

  private class PopupTrigger extends MouseAdapter {

    @Override
    public void mouseReleased(MouseEvent e) {
      if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
        int x = e.getX();
        int y = e.getY();
        TreePath path = tree2.getPathForLocation(x, y);

        popup.show(tree2, x, y);
        // m_clickedPath = path;
      }
    }
  }

  private final JTree tree1;
  private final JTree tree2;
  private final JPopupMenu popup;

  private boolean choice1;
  private boolean choice2;

  public SwingTree(String title) {
    super(title);
    Box box = Box.createHorizontalBox();

    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

    for (int i = 0; i < 5; i++) {
      DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("Parent" + i);
      for (int j = 0; j < 2; j++) {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode("Child" + i + j);
        for (int k = 0; k < 3; k++) {
          DefaultMutableTreeNode gNode = new DefaultMutableTreeNode("grandChild" + i + j + k);
          childNode.add(gNode);
        }
        treeNode.add(childNode);
      }
      root.add(treeNode);
    }
    tree1 = new JTree(root);
    tree1.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    //	   tree1.addMouseListener(listener);
    tree1.setName("tree1");
    JScrollPane scrollPane1 = new JScrollPane(tree1);
    scrollPane1.setName("scrollPane1");

    // tree 2
    DefaultMutableTreeNode root2 = new DefaultMutableTreeNode("Root");

    for (int i = 0; i < 5; i++) {
      DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("Item " + i);
      for (int j = 0; j < 2; j++) {
        DefaultMutableTreeNode leafNode = new DefaultMutableTreeNode("Node " + i + j);
        treeNode.add(leafNode);
      }
      root2.add(treeNode);
    }
    tree2 = new JTree(root2);
    tree2.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
    //	    tree2.addMouseListener(listener);
    tree2.setName("tree2");
    // add popop menu
    popup = new JPopupMenu();
    JMenuItem menuItem1 = new JMenuItem("choice1");
    menuItem1.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            choice1 = !choice1;
          }
        });
    popup.add(menuItem1);

    JMenuItem menuItem2 = new JMenuItem("choice2");
    menuItem2.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            choice2 = !choice2;
          }
        });
    popup.add(menuItem2);
    // tree2.add(popup);
    tree2.addMouseListener(new PopupTrigger());

    JScrollPane scrollPane2 = new JScrollPane(tree2);
    scrollPane2.setName("scrollPane2");
    box.add(scrollPane1, BorderLayout.WEST);
    box.add(scrollPane2, BorderLayout.EAST);

    add(box, BorderLayout.CENTER);
    setSize(400, 250);
  }

  public JTree getTree1() {
    return tree1;
  }

  public JTree getTree2() {
    return tree2;
  }

  public boolean isChoice1() {
    return choice1;
  }

  public boolean isChoice2() {
    return choice2;
  }
}
