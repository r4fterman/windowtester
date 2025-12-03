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
package com.windowtester.swing.recorder;

import abbot.script.Resolver;
import abbot.script.Step;
import abbot.util.AWT;
import com.windowtester.recorder.event.ISemanticEventListener;
import com.windowtester.recorder.event.IUISemanticEvent;
import com.windowtester.recorder.event.UISemanticEventFactory;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.MenuItem;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JTree;

/***
 * Override the createStep methods and call the WindowTester semantic
 * event generation
 *
 */
public class ComponentRecorder extends abbot.editor.recorder.ComponentRecorder {

  // A list of semantic event listeners
  private List<ISemanticEventListener> listenerList;

  protected boolean doneEventGeneration = false;

  public ComponentRecorder(Resolver resolver) {
    super(resolver);
  }

  @Override
  protected boolean parseWindowEvent(AWTEvent event) {
    if (event.getID() == ComponentEvent.COMPONENT_SHOWN) {
      // create semantic event
      IUISemanticEvent semanticEvent =
          UISemanticEventFactory.createShellShowingEvent((Window) event.getSource());
      notify(semanticEvent);
    }
    return super.parseWindowEvent(event);
  }

  @Override
  protected Step createStep() {
    return super.createStep();
  }

  @Override
  protected Step createAWTMenuSelection(Component parent, MenuItem menuItem, boolean isPopup) {
    return super.createAWTMenuSelection(parent, menuItem, isPopup);
  }

  @Override
  protected Step createClick(Component target, int x, int y, int mods, int count) {
    // windowtester semantic event generation
    IUISemanticEvent semanticEvent =
        UISemanticEventFactory.createWidgetSelectionEvent(target, x, y, count, getButton());
    notify(semanticEvent);

    return super.createClick(target, x, y, mods, count);
  }

  @Override
  protected Step createDrag(Component comp, int x, int y) {
    return super.createDrag(comp, x, y);
  }

  @Override
  protected Step createDrop(Component comp, int x, int y) {
    return super.createDrop(comp, x, y);
  }

  @Override
  protected Step createInputMethod(List<Integer> codes, String text) {
    return super.createInputMethod(codes, text);
  }

  @Override
  protected Step createKey(Component comp, char keychar, int mods) {
    // Create semantic event and notify listener
    IUISemanticEvent semanticEvent = UISemanticEventFactory.createKeyDownEvent(comp, keychar);
    notify(semanticEvent);

    return super.createKey(comp, keychar, mods);
  }

  @Override
  protected Step createMenuSelection(Component menuItem) {
    // Create semantic event and notify listener
    IUISemanticEvent semanticEvent =
        UISemanticEventFactory.createMenuSelectionEvent(menuItem, getX(), getY());
    notify(semanticEvent);

    return super.createMenuSelection(menuItem);
  }

  @Override
  protected Step createPopupMenuSelection(Component invoker, int x, int y, Component menuItem) {
    // Create semantic event and notify listener
    if (!doneEventGeneration) {
      IUISemanticEvent semanticEvent = null;
      if (invoker != null) {
        semanticEvent =
            UISemanticEventFactory.createContextMenuSelectionEvent(
                invoker, x, y, (JMenuItem) menuItem);
      } else { // sometimes invoker is lost
        Component parent = null;
        Component popup = null;
        popup = menuItem.getParent();
        parent = AWT.getInvoker(popup);

        if (parent instanceof JTree) {
          int row = ((JTree) parent).getMinSelectionRow();
          semanticEvent =
              UISemanticEventFactory.createTreeItemContextMenuSelectionEvent(
                  (JTree) parent, row, (JMenuItem) menuItem);
        }
        if (parent instanceof JTable) {
          int row = ((JTable) parent).getSelectedRow();
          int col = ((JTable) parent).getSelectedColumn();
          semanticEvent =
              UISemanticEventFactory.createTableContextMenuSelectionEvent(
                  (JTable) parent, (JMenuItem) menuItem, row, col);
        }
      }
      notify(semanticEvent);
    }
    return super.createPopupMenuSelection(invoker, x, y, menuItem);
  }

  @Override
  protected Step createWindowEvent(Window window, boolean isClose) {
    // create semantic event and notify listener
    if (isClose) {
      IUISemanticEvent semanticEvent = UISemanticEventFactory.createShellDisposedEvent(window);
      notify(semanticEvent);
    }

    return super.createWindowEvent(window, isClose);
  }

  public void addListener(ISemanticEventListener listener) {
    List<ISemanticEventListener> listeners = getListeners();
    if (listeners.contains(listener)) {
      System.out.println("multiple adds of listener: " + listener);
    } else {
      listeners.add(listener);
    }
  }

  /**
   * Get the registered event listeners.
   *
   * @return a list of registered listeners
   */
  public List<ISemanticEventListener> getListeners() {
    if (listenerList == null) {
      listenerList = new ArrayList<>();
    }
    return listenerList;
  }

  public void removeListener(ISemanticEventListener listener) {
    List<ISemanticEventListener> listeners = getListeners();
    if (listeners.contains(listener)) {
      System.out.println("listener removed that was not registered: " + listener);
    } else {
      listeners.remove(listener);
    }
  }

  public void notify(IUISemanticEvent semanticEvent) {
    for (ISemanticEventListener iSemanticEventListener : getListeners()) {
      iSemanticEventListener.notify(semanticEvent);
    }
  }
}
