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
package com.windowtester.internal.swing;

import abbot.tester.ActionFailedException;
import abbot.util.AWT;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.Point;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;

public class DelayUIDriverSwing extends UIDriverSwing {

  private static boolean printMessage = false;

  /**
   * Create an instance (using the default settings).
   */
  public DelayUIDriverSwing() {
    // TODO: Load settings from file
  }

  @Override
  public Component click(int clickCount, Component component, int x, int y, int mask) {
    // first move the mouse to the point of interest:
    mouseMove(component);
    return super.click(clickCount, component, x, y, mask);
  }

  @Override
  public Component clickComboBox(JComboBox<?> combobox, String labelOrPath, int clickCount)
      throws ActionFailedException {
    // first move the mouse to the point of interest:
    mouseMove(combobox);
    return super.clickComboBox(combobox, labelOrPath, clickCount);
  }

  @Override
  public Component clickListItem(int clickCount, JList<?> list, String labelOrPath, int mask)
      throws ActionFailedException {
    // first move the mouse to the point of interest:
    mouseMove(list);
    return super.clickListItem(clickCount, list, labelOrPath, mask);
  }

  @Override
  public Component clickTable(
      int clickCount, JTable table, int rowIndex, int columnIndex, int mask) {
    mouseMove(table);
    return super.clickTable(clickCount, table, rowIndex, columnIndex, mask);
  }

  @Override
  public void mouseMove(Component component, int x, int y) {
    var mouseLocation = AWT.getLocationOnScreen(component);

    try {
      var pointerInfo = MouseInfo.getPointerInfo();
      var cursorLocation = pointerInfo.getLocation();

      var target = new Point(mouseLocation.x + x, mouseLocation.y + y);
      var path = getPath(cursorLocation, target);
      for (Point point : path) {
        super.mouseMove(point.x, point.y);
      }
    } catch (HeadlessException e) {
      if (!printMessage) {
        System.out.println(
            "Mouse Delay not supported, turn off Mouse Delay in"
                + " Window->Preferences->WindowTester->Playback");
        printMessage = true;
      }
    }
  }

  @Override
  public void enterText(String txt) throws ActionFailedException {
    // enter text one char at a time, pausing as we go
    for (int i = 0; i <= txt.length() - 1; ++i) {
      keyClick(txt.charAt(i));
    }
  }

  public static Point[] getPath(Point p1, Point p2) {
    var numSteps = getNumSteps(p1, p2);
    return getPath(p1, p2, numSteps + 3);
  }

  private static int getNumSteps(Point p1, Point p2) {
    var dx = Math.abs(p1.x - p2.x);
    var dy = Math.abs(p1.y - p2.y);
    return Math.max(dx, dy);
  }

  public static Point[] getPath(Point p1, Point p2, int numOfSteps) {
    var xSteps = getSteps(p1.x, p2.x, numOfSteps);
    var ySteps = getSteps(p1.y, p2.y, numOfSteps);

    var points = new Point[xSteps.length];
    for (int i = 0; i < xSteps.length; ++i) {
      points[i] = new Point(xSteps[i], ySteps[i]);
    }

    return points;
  }

  static int[] getSteps(int start, int stop, int numOfSteps) {
    var fSteps = new float[numOfSteps];
    var steps = new int[numOfSteps];
    var delta = stop - start;
    var increment = delta / numOfSteps;

    fSteps[0] = start;
    for (int i = 1; i < numOfSteps; ++i) {
      fSteps[i] = fSteps[i - 1] + increment;
    }
    for (int i = 0; i < numOfSteps; ++i) {
      steps[i] = Math.round(fSteps[i]);
    }
    // sanity check last coord:
    steps[numOfSteps - 1] = stop;
    return steps;
  }
}
