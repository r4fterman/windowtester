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
package com.windowtester.recorder.event;

import abbot.script.ArgumentParser;
import abbot.tester.ComponentLocation;
import abbot.tester.ComponentTester;
import abbot.tester.JTreeLocation;
import abbot.tester.KeyStrokeMap;
import abbot.util.AWT;
import com.windowtester.internal.runtime.IWidgetIdentifier;
import com.windowtester.internal.swing.WidgetLocatorService;
import com.windowtester.internal.swing.util.ComponentAccessor;
import com.windowtester.internal.swing.util.TextUtils;
import com.windowtester.internal.tester.swing.JTableTester;
import com.windowtester.internal.tester.swing.JTreeTester;
import com.windowtester.recorder.event.user.SemanticComboSelectionEvent;
import com.windowtester.recorder.event.user.SemanticKeyDownEvent;
import com.windowtester.recorder.event.user.SemanticListSelectionEvent;
import com.windowtester.recorder.event.user.SemanticMenuSelectionEvent;
import com.windowtester.recorder.event.user.SemanticShellClosingEvent;
import com.windowtester.recorder.event.user.SemanticShellDisposedEvent;
import com.windowtester.recorder.event.user.SemanticShellShowingEvent;
import com.windowtester.recorder.event.user.SemanticTabbedPaneSelectionEvent;
import com.windowtester.recorder.event.user.SemanticTableSelectionEvent;
import com.windowtester.recorder.event.user.SemanticTreeItemSelectionEvent;
import com.windowtester.recorder.event.user.SemanticWidgetSelectionEvent;
import com.windowtester.recorder.event.user.TreeEventType;
import com.windowtester.recorder.event.user.UISemanticEvent;
import com.windowtester.recorder.event.user.UISemanticEvent.EventInfo;
import com.windowtester.runtime.WidgetLocator;
import com.windowtester.runtime.swing.SwingWidgetLocator;
import com.windowtester.runtime.swing.locator.AbstractPathLocator;
import com.windowtester.runtime.swing.locator.JComboBoxLocator;
import com.windowtester.runtime.swing.locator.JListLocator;
import com.windowtester.runtime.swing.locator.JTabbedPaneLocator;
import com.windowtester.runtime.swing.locator.JTableItemLocator;
import com.windowtester.runtime.swing.locator.JTextComponentLocator;
import com.windowtester.runtime.swing.locator.JTreeItemLocator;
import com.windowtester.runtime.swing.locator.NamedWidgetLocator;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.TreePath;

/**
 * Factory for creating semantic events from AWT events using the new widgetLocator
 * scheme.
 */
public class UISemanticEventFactory {

  // the last seen widget --- whereby seen we mean has had info extracted...
  private static Component _lastWidget;
  // the last calculated widget locator
  private static WidgetLocator _lastWidgetLocator;

  private static final ComponentTester _menuItemTester = ComponentTester.getTester(JMenuItem.class);
  private static final ComponentTester _treeTester = ComponentTester.getTester(JTree.class);

  /**
   * Construct a SemanticKeyDownEvent
   *
   * @return a semantic event
   */
  public static SemanticKeyDownEvent createKeyDownEvent(Component comp, char keychar) {
    EventInfo info = extractInfo(comp, 0, 0, 1);
    SemanticKeyDownEvent keyDown = new SemanticKeyDownEvent(info);

    keyDown.setKey(keychar);
    KeyStroke ks = KeyStrokeMap.getKeyStroke(keychar);
    keyDown.setKeyCode(ks.getKeyCode());

    return keyDown;
  }

  public static SemanticKeyDownEvent createKeyDownEvent(Component comp, int code, char keychar) {
    EventInfo info = extractInfo(comp, 0, 0, 0);
    SemanticKeyDownEvent keyDown = new SemanticKeyDownEvent(info);
    keyDown.setKey(keychar);
    keyDown.setKeyCode(code);
    return keyDown;
  }

  /**
   * Construct a SemanticMenuSelectionEvent.
   *
   * @return a semantic event
   */
  public static SemanticMenuSelectionEvent createMenuSelectionEvent(
      Component menuItem, int x, int y) {

    JMenuItem item = (JMenuItem) menuItem;
    String label = ComponentAccessor.extractMenuItemLabel(item);
    String pathString = ComponentAccessor.extractMenuPath(item) + "/" + label;

    EventInfo info = new EventInfo();
    // NOTE: we use the root to find
    // 12/27: kp: why should we use root? - we need locator for menu item, so use the menuitem
    AbstractPathLocator locator = (AbstractPathLocator) inferIdentifyingInfo(item);
    // update locator path string
    //	locator.setPath(pathString);

    info.cls = item.getClass().getName();
    info.hierarchyInfo = locator;
    info.button = 1;
    info.x = x;
    info.y = y;

    SemanticMenuSelectionEvent menuSelect = new SemanticMenuSelectionEvent(info);

    menuSelect.setItemLabel(label);
    menuSelect.setPath(pathString);

    return menuSelect;
  }

  /**
   * Create a context menu selection event
   * @param item    - menu item
   * @return SemanticMenuSelectionEvent
   */
  public static UISemanticEvent createContextMenuSelectionEvent(
      Component invoker, int x, int y, JMenuItem item) {
    String pathString = _menuItemTester.deriveTag(item);
    pathString = TextUtils.fixTabs(pathString);

    EventInfo info = new EventInfo();
    info.cls = invoker.getClass().getName();
    info.hierarchyInfo = inferIdentifyingInfo(invoker);
    info.button = 3;
    info.x = x;
    info.y = y;

    SemanticMenuSelectionEvent menuSelect = new SemanticMenuSelectionEvent(info);

    String label = ComponentAccessor.extractMenuItemLabel(item);
    menuSelect.setItemLabel(label);
    menuSelect.setPath(pathString);
    return menuSelect;
  }

  /**
   * Create a tree selection semantic event
   */
  public static UISemanticEvent createTreeItemSelectionEvent(
      JTree invoker, int x, int y, String mask, int clkCount, int button) {

    ComponentLocation location = _treeTester.getLocation(invoker, new Point(x, y));
    TreePath path = ((JTreeLocation) location).getPath(invoker);

    TreePath pathString = JTreeTester.pathToStringPath(invoker, path);
    String[] nodeNames = ComponentAccessor.parseTreePath(pathString.toString());
    String nodePath = ComponentAccessor.assemblePath(nodeNames);

    EventInfo info = extractInfo(invoker, x, y, button);
    /*
     * Extract guarantees uniqueness.
     * Now we can set up a JTreeItemLocator based on the inferred tree locator.
     */
    IWidgetIdentifier locator = info.hierarchyInfo;
    SwingWidgetLocator parent = getParentInfo(locator);
    int index = getIndex(info.hierarchyInfo);
    // check if a tree is named
    String name = invoker.getName();
    if (name != null) {
      parent = new NamedWidgetLocator(name);
      index = WidgetLocator.UNASSIGNED;
    }

    // swap in our item locator
    info.hierarchyInfo = new JTreeItemLocator(nodePath, index, parent);

    SemanticTreeItemSelectionEvent treeItemSelect;
    if (clkCount == 1) {
      treeItemSelect = new SemanticTreeItemSelectionEvent(info, TreeEventType.SINGLE_CLICK);
    } else {
      treeItemSelect = new SemanticTreeItemSelectionEvent(info, TreeEventType.DOUBLE_CLICK);
    }

    treeItemSelect.setClicks(clkCount);

    if (mask != null) {
      treeItemSelect.setMask(mask);
    }

    treeItemSelect.setItemLabel(nodeNames[nodeNames.length - 1]);
    treeItemSelect.setItemPath(nodePath);
    return treeItemSelect;
  }

  private static SwingWidgetLocator getParentInfo(IWidgetIdentifier locator) {
    if (locator instanceof SwingWidgetLocator) { // ugh
      return (SwingWidgetLocator) ((SwingWidgetLocator) locator).getParentInfo();
    }
    return null;
  }

  private static int getIndex(IWidgetIdentifier locator) {
    if (locator instanceof WidgetLocator) { // ugh
      return ((WidgetLocator) locator).getIndex();
    }
    return WidgetLocator.UNASSIGNED;
  }

  /**
   * Create a context menu selection on a tree item
   */
  public static UISemanticEvent createTreeItemContextMenuSelectionEvent(
      JTree invoker, int x, int y, JMenuItem item) {

    String menuPath = _menuItemTester.deriveTag(item);
    menuPath = TextUtils.fixTabs(menuPath);

    SemanticTreeItemSelectionEvent treeContextMenuSelect =
        (SemanticTreeItemSelectionEvent) createTreeItemSelectionEvent(invoker, x, y, null, 1, 3);
    treeContextMenuSelect.setContextMenuSelectionPath(menuPath);

    return treeContextMenuSelect;
  }

  /**
   * Create a context menu selection on a tree item
   */
  public static UISemanticEvent createTreeItemContextMenuSelectionEvent(
      JTree invoker, int row, JMenuItem item) {

    EventInfo info = extractInfo(invoker, -1, -1, 3);
    SemanticTreeItemSelectionEvent treeItemSelect =
        new SemanticTreeItemSelectionEvent(info, TreeEventType.SINGLE_CLICK);

    treeItemSelect.setClicks(1);
    JTreeLocation location = new JTreeLocation(row);
    TreePath path = location.getPath(invoker);
    TreePath pathString = JTreeTester.pathToStringPath(invoker, path);
    String[] nodeNames = ComponentAccessor.parseTreePath(pathString.toString());
    String nodePath = ComponentAccessor.assemblePath(nodeNames);

    treeItemSelect.setItemLabel(nodeNames[nodeNames.length - 1]);
    treeItemSelect.setItemPath(nodePath);

    String menuPath = _menuItemTester.deriveTag(item);
    menuPath = TextUtils.fixTabs(menuPath);
    treeItemSelect.setContextMenuSelectionPath(menuPath);

    return treeItemSelect;
  }

  public static UISemanticEvent createTabbedPaneSelectionEvent(
      JTabbedPane invoker, int x, int y, int index) {

    EventInfo info = extractInfo(invoker, x, y, 1);
    String tabLabel = invoker.getTitleAt(index);

    //	swap in the custom tabbed pane locator
    SwingWidgetLocator parentInfo = getParentInfo(info.hierarchyInfo);
    int idx = getIndex(info.hierarchyInfo);
    // check whether a component is named
    String name = invoker.getName();
    if (name != null) {
      parentInfo = new NamedWidgetLocator(name);
      idx = WidgetLocator.UNASSIGNED;
    }
    info.hierarchyInfo = new JTabbedPaneLocator(tabLabel, idx, parentInfo);

    SemanticTabbedPaneSelectionEvent tabbedPaneEvent = new SemanticTabbedPaneSelectionEvent(info);
    tabbedPaneEvent.setIndex(index);
    tabbedPaneEvent.setTabLabel(tabLabel);

    return tabbedPaneEvent;
  }

  /**
   * Create a table selection semantic event
   */
  public static UISemanticEvent createTableSelectionEvent(
      JTable invoker, int x, int y, String mask, int clkCount, int button) {

    EventInfo info = extractInfo(invoker, x, y, button);

    Point where = new Point(x, y);
    int row = invoker.rowAtPoint(where);
    int col = invoker.columnAtPoint(where);
    String label = JTableTester.valueToString(invoker, row, col);

    // build and hook up table item locator
    SwingWidgetLocator parentInfo = getParentInfo(info.hierarchyInfo);
    int index = getIndex(info.hierarchyInfo);
    // check if the table is named, if yes create named widget locator
    String name = invoker.getName();
    if (name != null) {
      parentInfo = new NamedWidgetLocator(name);
      index = WidgetLocator.UNASSIGNED;
    }
    info.hierarchyInfo = new JTableItemLocator(new Point(row, col), null, index, parentInfo);

    SemanticTableSelectionEvent tableSelect = new SemanticTableSelectionEvent(info);

    tableSelect.setClicks(clkCount);
    tableSelect.setItemLabel(label);
    tableSelect.setTableItemRow(row);
    tableSelect.setTableItemCol(col);
    if (mask != null) {
      tableSelect.setMask(mask);
    }

    return tableSelect;
  }

  /**
   * create a context menu selection for a table item.
   */
  public static UISemanticEvent createTableContextMenuSelectionEvent(
      JTable table, int x, int y, JMenuItem item) {
    String menuPath = _menuItemTester.deriveTag(item);
    menuPath = TextUtils.fixTabs(menuPath);

    SemanticTableSelectionEvent tableContextMenuSelect =
        (SemanticTableSelectionEvent) createTableSelectionEvent(table, x, y, null, 1, 3);

    tableContextMenuSelect.setContextMenuSelectionPath(menuPath);
    return tableContextMenuSelect;
  }

  /**
   * Create a context menu selection for a table item.
   */
  public static UISemanticEvent createTableContextMenuSelectionEvent(
      JTable table, JMenuItem item, int row, int col) {
    // get menu label - not path
    String menuPath = _menuItemTester.deriveTag(item);

    menuPath = TextUtils.fixTabs(menuPath);

    EventInfo info = extractInfo(table, -1, -1, 3);
    SemanticTableSelectionEvent tableContextMenuSelect = new SemanticTableSelectionEvent(info);
    tableContextMenuSelect.setClicks(1);
    String label = JTableTester.valueToString(table, row, col);
    tableContextMenuSelect.setItemLabel(label);
    tableContextMenuSelect.setTableItemRow(row);

    tableContextMenuSelect.setContextMenuSelectionPath(menuPath);
    return tableContextMenuSelect;
  }

  /**
   * Create a combo box selection semantic event.
   */
  public static SemanticComboSelectionEvent createComboSelectionEvent(
      JComboBox<?> combo, String label) {

    EventInfo info = extractInfo(combo, 1, 1, 1);
    // swap in our combo locator (post identification)
    SwingWidgetLocator parentInfo = getParentInfo(info.hierarchyInfo);
    int index = getIndex(info.hierarchyInfo);
    // check if a combo is named
    String name = combo.getName();
    if (name != null) {
      parentInfo = new NamedWidgetLocator(name);
      index = WidgetLocator.UNASSIGNED;
    }
    info.hierarchyInfo = new JComboBoxLocator(label, index, parentInfo);

    SemanticComboSelectionEvent comboEvent = new SemanticComboSelectionEvent(info);

    comboEvent.setSelection(label);
    return comboEvent;
  }

  /**
   * create a list selection semantic event
   */
  public static UISemanticEvent createListSelectionEvent(
      JList list, int x, int y, int mods, int count, int button) {

    EventInfo info = new EventInfo();
    info.cls = list.getClass().getName();
    info.hierarchyInfo = inferIdentifyingInfo(list);
    info.button = button;
    info.x = x;
    info.y = y;

    int index = list.locationToIndex(new Point(x, y));

    Object value = list.getModel().getElementAt(index);
    Component cr =
        list.getCellRenderer().getListCellRendererComponent(list, value, index, false, false);
    String string = null;
    String item;
    if (cr instanceof javax.swing.JLabel) {
      string = ((javax.swing.JLabel) cr).getText();
      if (string != null) {
        string = string.trim();
      }
    }
    if (!"".equals(string) && !ArgumentParser.isDefaultToString(string)) {
      item = string;
    } else {
      item = list.getModel().getElementAt(index).toString();
    }
    // swap in the custom list locator
    SwingWidgetLocator parentInfo = getParentInfo(info.hierarchyInfo);
    int idx = getIndex(info.hierarchyInfo);
    // check if the name is set
    String name = list.getName();
    if (name != null) { // create a named widget locator
      parentInfo = new NamedWidgetLocator(name);
      idx = WidgetLocator.UNASSIGNED;
    }

    info.hierarchyInfo = new JListLocator(item, idx, parentInfo);

    SemanticListSelectionEvent listSelect = new SemanticListSelectionEvent(info);
    listSelect.setClicks(count);
    if (mods != MouseEvent.BUTTON1_DOWN_MASK) {
      listSelect.setMask(AWT.getMouseModifiers(mods));
    }
    listSelect.setItem(item);

    return listSelect;
  }

  /**
   * create a widget selection semantic event
   */
  public static UISemanticEvent createWidgetSelectionEvent(
      Component widget, int x, int y, int count, int button) {

    EventInfo info = extractInfo(widget, x, y, button);

    SemanticWidgetSelectionEvent widgetSelect = new SemanticWidgetSelectionEvent(info);
    widgetSelect.setClicks(count);
    widgetSelect.setItemLabel(ComponentAccessor.extractWidgetLabel(widget));
    return widgetSelect;
  }

  public static UISemanticEvent createTextComponentSelectionEvent(
      Component widget, int x, int y, int count, int button, int caret) {

    EventInfo info = new EventInfo();
    info.cls = widget.getClass().getName();
    info.hierarchyInfo = inferIdentifyingInfo(widget);
    info.button = button;
    info.x = x;
    info.y = y;

    //	swap in custom list locator if component is JTextPane
    // com.windowtester.runtime.swing.SwingWidgetLocator parentInfo =
    // getParentInfo(info.hierarchyInfo);
    // set caret if necessary, a click not in the start of the field
    if (widget instanceof JTextField) {
      if (((JTextField) widget).getText().length() == caret) {
        caret = 0;
      }
    }
    // if it is a NamedWidgetLocator, there is no set caret
    if (info.hierarchyInfo instanceof JTextComponentLocator) {
      if (caret != 0) {
        ((JTextComponentLocator) info.hierarchyInfo).setCaretPosition(caret);
      } else {
        ((JTextComponentLocator) info.hierarchyInfo).setCaretPosition(WidgetLocator.UNASSIGNED);
      }
    }

    SemanticWidgetSelectionEvent widgetSelect = new SemanticWidgetSelectionEvent(info);
    widgetSelect.setClicks(count);
    return widgetSelect;
  }

  /**
   * Create a shell-closing semantic event for the window close AWT events
   */
  public static UISemanticEvent createShellClosingEvent(Window window) {
    EventInfo info = extractInfo(window, 0, 0, 1);
    var text = ComponentAccessor.extractTitle(window);
    return new SemanticShellClosingEvent(info, text);
  }

  /**
   * Create a shell-disposed semantic event for window close AWT events
   */
  public static UISemanticEvent createShellDisposedEvent(Window window) {
    EventInfo info = extractInfo(window, 0, 0, 1);
    var text = ComponentAccessor.extractTitle(window);
    return new SemanticShellDisposedEvent(info, text);
  }

  /**
   * Create a shell showing semantic event.
   */
  public static UISemanticEvent createShellShowingEvent(Window window) {
    EventInfo info = extractInfo(window, 0, 0, 1);
    var text = ComponentAccessor.extractTitle(window);
    return new SemanticShellShowingEvent(info, text);
  }

  /**
   * Extract relevant info.
   */
  private static EventInfo extractInfo(Component widget, int x, int y, int button) {
    EventInfo info = new EventInfo();
    info.cls = widget.getClass().getName();
    info.button = button;
    info.x = x;
    info.y = y;

    /*
     * here we do a little caching to speed things up
     */
    if (widget == _lastWidget && _lastWidgetLocator != null) {
      info.hierarchyInfo = _lastWidgetLocator;
    } else { // do it the hard way:
      info.hierarchyInfo = inferIdentifyingInfo(widget);
      // and update the cache
      _lastWidgetLocator = (WidgetLocator) info.hierarchyInfo;
      _lastWidget = widget;
    }
    return info;
  }

  private static WidgetLocator inferIdentifyingInfo(Component widget) {
    return new WidgetLocatorService().inferIdentifyingInfo(widget);
  }
}
