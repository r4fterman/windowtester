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

// TestTree4.java
// Another test to see how we can build a tree and customize its icons.
// This example does not affect the icons of other trees.
//

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class TestTree extends JFrame {

  public TestTree() {
    super("Custom Icon Example");
    setSize(350, 450);
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    // Build the hierarchy of containers & objects
    String[] schoolyard = {"School", "Playground", "Parking Lot", "Field"};
    String[] mainstreet = {"Grocery", "Shoe Shop", "Five & Dime", "Post Office"};
    String[] highway = {"Gas Station", "Convenience Store"};
    String[] housing = {"Victorian_blue", "Faux Colonial", "Victorian_white"};
    String[] housing2 = {"Mission", "Ranch", "Condo"};

    Map<String, String[]> homeHash = new HashMap<>();
    homeHash.put("Residential 1", housing);
    homeHash.put("Residential 2", housing2);

    Hashtable<String, Object> cityHash = new Hashtable<>();
    cityHash.put("School grounds", schoolyard);
    cityHash.put("Downtown", mainstreet);
    cityHash.put("Highway", highway);
    cityHash.put("Housing", homeHash);

    Hashtable<String, Map<String, Object>> worldHash = new Hashtable<>();
    worldHash.put("My First VRML World", cityHash);

    // Build our tree out of our big hashtable
    JTree tree1 = new JTree(worldHash);
    JTree tree2 = new JTree(worldHash);

    DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree2.getCellRenderer();
    renderer.setClosedIcon(new ImageIcon("door.closed.gif"));
    renderer.setOpenIcon(new ImageIcon("door.open.gif"));
    renderer.setLeafIcon(new ImageIcon("world.gif"));

    JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tree1, tree2);

    getContentPane().add(pane, BorderLayout.CENTER);
  }
}
