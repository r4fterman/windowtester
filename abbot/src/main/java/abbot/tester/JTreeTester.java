package abbot.tester;

import abbot.WaitTimedOutError;
import abbot.i18n.Strings;
import abbot.script.ArgumentParser;
import abbot.script.Condition;
import abbot.util.AWT;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

/**
 * Provide operations on a JTree component. The JTree substructure is a "row", and JTreeLocation
 * provides different identifiers for a row.
 * <ul>
 * <li>Select an item by row index
 * <li>Select an item by tree path (the string representation of the full
 * path).
 * </ul>
 *
 * @see abbot.tester.JTreeLocation
 */
// TODO: multi-select
// TODO: expand/collapse actions
public class JTreeTester extends JComponentTester {

  /**
   * Returns whether the given point is in one of the JTree's node expansion controls.
   */
  public static boolean isLocationInExpandControl(JTree tree, int x, int y) {
    int row = tree.getRowForLocation(x, y);
    if (row == -1) {
      row = tree.getClosestRowForLocation(x, y);
      if (row != -1) {
        Rectangle rect = tree.getRowBounds(row);
        if (row == tree.getRowCount() - 1 && y >= rect.y + rect.height) {
          return false;
        }
        // An approximation: use a square area to the left of the row
        // bounds.
        TreePath path = tree.getPathForRow(row);
        if (path == null || tree.getModel().isLeaf(path.getLastPathComponent())) {
          return false;
        }

        if (tree.getUI() instanceof BasicTreeUI) {
          try {
            java.lang.reflect.Method method =
                BasicTreeUI.class.getDeclaredMethod(
                    "isLocationInExpandControl", TreePath.class, int.class, int.class);
            method.setAccessible(true);
            Object b = method.invoke(tree.getUI(), path, x, y);
            return b.equals(Boolean.TRUE);
          } catch (Exception e) {
          }
        }
        // fall back to best guess
        // return x >= rect.x - rect.height && x < rect.x;
        String msg = "Can't determine location of tree expansion " + "control for " + tree.getUI();
        throw new RuntimeException(msg);
      }
    }
    return false;
  }

  /**
   * Return the {@link String} representation of the final component of the given {@link TreePath},
   * or
   * <code>null</code> if one can not be obtained.  Assumes the path is visible.
   */
  static String valueToString(JTree tree, TreePath path) {
    Object value = path.getLastPathComponent();
    int row = tree.getRowForPath(path);
    // The default renderer will rely on JTree.convertValueToText
    Component cr =
        tree.getCellRenderer()
            .getTreeCellRendererComponent(
                tree,
                value,
                false,
                tree.isExpanded(row),
                tree.getModel().isLeaf(value),
                row,
                false);
    String string = null;
    if (cr instanceof JLabel crLabel) {
      String label = crLabel.getText();
      if (label != null) {
        label = label.trim();
      }
      if (!"".equals(label) && !ArgumentParser.isDefaultToString(label)) {
        string = label;
      }
    }
    if (string == null) {
      string =
          tree.convertValueToText(
              value, false, tree.isExpanded(row), tree.getModel().isLeaf(value), row, false);
      if (ArgumentParser.isDefaultToString(string)) {
        string = null;
      }
    }
    if (string == null) {
      String s = ArgumentParser.toString(value);
      string = Objects.equals(s, ArgumentParser.DEFAULT_TOSTRING) ? null : s;
    }
    return string;
  }

  /**
   * Return the String representation of the given TreePath, or null if one can not be obtained.
   * Assumes the path is visible.
   */
  public static TreePath pathToStringPath(JTree tree, TreePath path) {
    if (path == null) {
      return null;
    }

    String string = valueToString(tree, path);
    if (string != null) {
      // Prepend the parent value, if any
      if (path.getPathCount() > 1) {
        TreePath parent = pathToStringPath(tree, path.getParentPath());
        if (parent == null) {
          return null;
        }
        return parent.pathByAddingChild(string);
      }
      return new TreePath(string);
    }
    return null;
  }

  /**
   * Click at the given location.  If the location indicates a path, ensure it is visible first.
   */
  @Override
  public void actionClick(Component c, ComponentLocation location) {
    if (location instanceof JTreeLocation treeLocation) {
      TreePath path = treeLocation.getPath((JTree) c);
      if (path != null) {
        makeVisible(c, path);
      }
    }
    super.actionClick(c, location);
  }

  /**
   * Select the given row.  If the row is already selected, does nothing.
   */
  public void actionSelectRow(Component component, ComponentLocation componentLocation) {
    JTree tree = (JTree) component;
    if (componentLocation instanceof JTreeLocation treeLocation) {
      TreePath path = treeLocation.getPath((JTree) component);
      if (path == null) {
        String msg = Strings.get("tester.JTree.path_not_found", new Object[] {componentLocation});
        throw new LocationUnavailableException(msg);
      }
      makeVisible(component, path);
    }
    Point where = componentLocation.getPoint(component);
    int row = tree.getRowForLocation(where.x, where.y);
    if (tree.getLeadSelectionRow() != row || tree.getSelectionCount() != 1) {
      // NOTE: the row bounds *do not* include the expansion handle
      Rectangle rect = tree.getRowBounds(row);
      // NOTE: if there's no icon, this may start editing
      actionClick(tree, rect.x + 1, rect.y + rect.height / 2);
    }
  }

  /**
   * Select the given row.  If the row is already selected, does nothing. Equivalent to
   * actionSelectRow(c, new JTreeLocation(row)).
   */
  public void actionSelectRow(Component tree, int row) {
    actionSelectRow(tree, new JTreeLocation(row));
  }

  /**
   * Simple click on the given row.
   */
  public void actionClickRow(Component tree, int row) {
    actionClick(tree, new JTreeLocation(row));
  }

  /**
   * Click with modifiers on the given row.
   *
   * @deprecated Use the ComponentLocation version.
   */
  @Deprecated
  public void actionClickRow(Component tree, int row, String modifiers) {
    actionClick(tree, new JTreeLocation(row), AWT.getModifiers(modifiers));
  }

  /**
   * Multiple click on the given row.
   *
   * @deprecated Use the ComponentLocation version.
   */
  @Deprecated
  public void actionClickRow(Component c, int row, String modifiers, int count) {
    actionClick(c, new JTreeLocation(row), AWT.getModifiers(modifiers), count);
  }

  /**
   * Make the given path visible, if possible, and returns whether any action was taken.
   *
   * @throws LocationUnavailableException if no corresponding path can be found.
   */
  protected boolean makeVisible(Component c, TreePath path) {
    return makeVisible(c, path, false);
  }

  private boolean makeVisible(Component c, TreePath path, boolean expandWhenFound) {
    JTree tree = (JTree) c;
    // Match, make visible, and expand the path one component at a time,
    // from uppermost ancestor on down, since children may be lazily
    // loaded/created
    boolean changed = false;
    if (path.getPathCount() > 1) {
      changed = makeVisible(c, path.getParentPath(), true);
      if (changed) {
        waitForIdle();
      }
    }

    TreePath realPath = JTreeLocation.findMatchingPath(tree, path);
    if (expandWhenFound) {
      if (!tree.isExpanded(realPath)) {
        // Use this method instead of a toggle action to avoid
        // any component visibility requirements
        invokeAndWait(() -> tree.expandPath(realPath));
      }
      Object o = realPath.getLastPathComponent();
      // Wait for a child to show up
      try {
        wait(
            new Condition() {
              @Override
              public boolean test() {
                return tree.getModel().getChildCount(o) != 0;
              }

              @Override
              public String toString() {
                return Strings.get("tester.Component.show_wait", new Object[] {path.toString()});
              }
            },
            componentDelay);
        changed = true;
      } catch (WaitTimedOutError e) {
        throw new LocationUnavailableException(e.getMessage());
      }
    }
    return changed;
  }

  /**
   * Ensure all elements of the given path are visible.
   */
  public void actionMakeVisible(Component c, TreePath path) {
    makeVisible(c, path);
  }

  /**
   * Select the given path, expanding parent nodes if necessary.
   */
  public void actionSelectPath(Component c, TreePath path) {
    actionSelectRow(c, new JTreeLocation(path));
  }

  /**
   * Change the open/closed state of the given row, if possible.
   *
   * @deprecated Use the ComponentLocation version instead.
   */
  @Deprecated
  public void actionToggleRow(Component c, int row) {
    actionToggleRow(c, new JTreeLocation(row));
  }

  /**
   * Change the open/closed state of the given row, if possible.
   */
  // NOTE: a reasonable assumption is that the toggle control is just to the
  // left of the row bounds and is roughly a square the dimensions of the
  // row height.  clicking in the center of that square should work.
  public void actionToggleRow(Component component, ComponentLocation componentLocation) {
    JTree tree = (JTree) component;
    // Alternatively, we can reflect into the UI and do a single click
    // on the appropriate expand location, but this is safer.
    if (tree.getToggleClickCount() != 0) {
      actionClick(
          tree, componentLocation, InputEvent.BUTTON1_DOWN_MASK, tree.getToggleClickCount());
    } else {
      // BasicTreeUI provides this method; punt if we can't find it
      if (!(tree.getUI() instanceof BasicTreeUI)) {
        throw new ActionFailedException("Can't toggle row for " + tree.getUI());
      }
      try {
        java.lang.reflect.Method method =
            BasicTreeUI.class.getDeclaredMethod("toggleExpandState", TreePath.class);
        method.setAccessible(true);
        Point where = componentLocation.getPoint(tree);
        method.invoke(tree.getUI(), tree.getPathForLocation(where.x, where.y));
      } catch (Exception e) {
        throw new ActionFailedException(e.toString());
      }
    }
  }

  /**
   * Determine whether a given path exists, expanding ancestor nodes as necessary to find it.
   *
   * @return Whether the given path on the given tree exists.
   */
  public boolean assertPathExists(Component tree, TreePath path) {
    try {
      makeVisible(tree, path);
      return true;
    } catch (LocationUnavailableException e) {
      return false;
    }
  }

  /**
   * Parse the String representation of a JTreeLocation into the actual JTreeLocation object.
   */
  @Override
  public ComponentLocation parseLocation(String encoded) {
    return new JTreeLocation().parse(encoded);
  }

  @Override
  public ComponentLocation getLocation(Component component, Point point) {
    JTree tree = (JTree) component;
    if (tree.getRowCount() == 0) {
      return new JTreeLocation(point);
    }
    Rectangle rect = tree.getRowBounds(tree.getRowCount() - 1);
    int maxY = rect.y + rect.height;
    if (point.y > maxY) {
      return new JTreeLocation(point);
    }

    // TODO: ignore clicks to the left of the expansion control, or maybe
    // embed them in the location.
    TreePath path = tree.getClosestPathForLocation(point.x, point.y);
    TreePath stringPath = pathToStringPath(tree, path);
    if (stringPath != null) {
      // if the root is hidden, drop it from the path
      if (!tree.isRootVisible()) {
        Object[] objs = stringPath.getPath();
        Object[] subs = new Object[objs.length - 1];
        System.arraycopy(objs, 1, subs, 0, subs.length);
        stringPath = new TreePath(subs);
      }
      return new JTreeLocation(stringPath);
    }
    int row = tree.getClosestRowForLocation(point.x, point.y);
    if (row != -1) {
      return new JTreeLocation(row);
    }
    return new JTreeLocation(point);
  }
}
