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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class ArrayListTransferHandler extends TransferHandler {

  private final DataFlavor serialListFlavor;

  private DataFlavor localListFlavor;
  private JList<String> source = null;
  private int[] indices = null;
  private int addIndex = -1; // Location where items were added
  private int addCount = 0; // Number of items added

  public ArrayListTransferHandler() {
    try {
      String localArrayListType =
          DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList";
      localListFlavor = new DataFlavor(localArrayListType);
    } catch (ClassNotFoundException e) {
      System.out.println("ArrayListTransferHandler: unable to create data flavor");
    }
    serialListFlavor = new DataFlavor(ArrayList.class, "ArrayList");
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean importData(JComponent c, Transferable t) {
    if (!canImport(c, t.getTransferDataFlavors())) {
      return false;
    }

    JList<String> target;
    List<String> alist;
    try {
      target = (JList<String>) c;
      if (hasLocalArrayListFlavor(t.getTransferDataFlavors())) {
        alist = (List<String>) t.getTransferData(localListFlavor);
      } else if (hasSerialArrayListFlavor(t.getTransferDataFlavors())) {
        alist = (List<String>) t.getTransferData(serialListFlavor);
      } else {
        return false;
      }
    } catch (UnsupportedFlavorException ufe) {
      System.out.println("importData: unsupported data flavor");
      return false;
    } catch (IOException ioe) {
      System.out.println("importData: I/O exception");
      return false;
    }

    // At this point we use the same code to retrieve the data
    // locally or serially.

    // We'll drop at the current selected index.
    int index = target.getSelectedIndex();

    // Prevent the user from dropping data back on itself.
    // For example, if the user is moving items #4,#5,#6 and #7 and
    // attempts to insert the items after item #5, this would
    // be problematic when removing the original items.
    // This is interpreted as dropping the same data on itself
    // and has no effect.
    if (source.equals(target)) {
      if (indices != null && index >= indices[0] - 1 && index <= indices[indices.length - 1]) {
        indices = null;
        return true;
      }
    }

    DefaultListModel<String> listModel = (DefaultListModel<String>) target.getModel();
    int max = listModel.getSize();
    if (index < 0) {
      index = max;
    } else {
      index++;
      if (index > max) {
        index = max;
      }
    }
    addIndex = index;
    addCount = alist.size();
    for (String s : alist) {
      listModel.add(index++, s);
    }
    return true;
  }

  @Override
  protected void exportDone(JComponent c, Transferable data, int action) {
    if ((action == MOVE) && (indices != null)) {
      DefaultListModel<String> model = (DefaultListModel<String>) source.getModel();

      // If we are moving items around in the same list, we
      // need to adjust the indices accordingly since those
      // after the insertion point have moved.
      if (addCount > 0) {
        for (int i = 0; i < indices.length; i++) {
          if (indices[i] > addIndex) {
            indices[i] += addCount;
          }
        }
      }
      for (int i = indices.length - 1; i >= 0; i--) {
        model.remove(indices[i]);
      }
    }
    indices = null;
    addIndex = -1;
    addCount = 0;
  }

  private boolean hasLocalArrayListFlavor(DataFlavor[] flavors) {
    if (localListFlavor == null) {
      return false;
    }

    for (DataFlavor flavor : flavors) {
      if (flavor.equals(localListFlavor)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasSerialArrayListFlavor(DataFlavor[] flavors) {
    if (serialListFlavor == null) {
      return false;
    }

    for (DataFlavor flavor : flavors) {
      if (flavor.equals(serialListFlavor)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean canImport(JComponent c, DataFlavor[] flavors) {
    if (hasLocalArrayListFlavor(flavors)) {
      return true;
    }
    return hasSerialArrayListFlavor(flavors);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Transferable createTransferable(JComponent c) {
    if (c instanceof JList) {
      source = (JList<String>) c;
      indices = source.getSelectedIndices();
      List<String> values = source.getSelectedValuesList();
      if (values == null || values.isEmpty()) {
        return null;
      }
      List<String> alist = new ArrayList<>(values.size());
      for (String str : values) {
        if (str == null) {
          str = "";
        }
        alist.add(str);
      }
      return new ListTransferable(alist);
    }
    return null;
  }

  @Override
  public int getSourceActions(JComponent c) {
    return COPY_OR_MOVE;
  }

  public class ListTransferable implements Transferable {

    private final List<String> data;

    public ListTransferable(List<String> alist) {
      data = alist;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (!isDataFlavorSupported(flavor)) {
        throw new UnsupportedFlavorException(flavor);
      }
      return data;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] {localListFlavor, serialListFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      if (localListFlavor.equals(flavor)) {
        return true;
      }
      return serialListFlavor.equals(flavor);
    }
  }
}
