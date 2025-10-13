package abbot.tester;

import abbot.i18n.Strings;
import abbot.script.ArgumentParser;
import java.awt.Component;
import java.awt.Point;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

/**
 * Provide actions and assertions for a {@link JList} component. The {@link JList} substructure is a
 * "row", and {@link JListLocation} provides different identifiers for a row.
 * <ul>
 * <li>Select an item by index
 * <li>Select an item by value (its string representation)
 * </ul>
 * Note that {@link JList} uses "index" and "value" in its API.  For
 * convenience, the <code>JListTester</code> API also provides "row" and
 * "item" as synonyms for "index".
 *
 * @see JListLocation
 */
// TODO multi-select

// Putting "Location" into ComponentTester removes the need for any subclass
// to duplicate click, click(mods), click(mods, count), as well as make
// specific methods unnecessary.

public class JListTester extends JComponentTester {

  /**
   * Convert the value in the list at the given index into a reasonable string representation, or
   * null if one can not be obtained.
   */
  static String valueToString(JList<?> list, int index) {
    Object value = list.getModel().getElementAt(index);
    ListCellRenderer<? super Object> cellRenderer =
        (ListCellRenderer<? super Object>) list.getCellRenderer();
    Component renderedListComponent =
        cellRenderer.getListCellRendererComponent(list, value, index, false, false);

    return convertToString(renderedListComponent, value);
  }

  private static String convertToString(Component renderedListComponent, Object value) {
    return convertListValueIntoString(renderedListComponent)
        .filter(v -> !v.isEmpty() && !ArgumentParser.isDefaultToString(v))
        .orElseGet(
            () -> {
              String string = ArgumentParser.toString(value);
              if (Objects.equals(string, ArgumentParser.DEFAULT_TOSTRING)) {
                return null;
              }
              return string;
            });
  }

  private static Optional<String> convertListValueIntoString(Component renderedListComponent) {
    if (renderedListComponent instanceof javax.swing.JLabel) {
      String rawString = ((javax.swing.JLabel) renderedListComponent).getText();
      if (rawString == null) {
        return Optional.empty();
      }
      return Optional.of(rawString.trim());
    }
    return Optional.empty();
  }

  /**
   * JList doesn't provide direct access to its contents, so make up for that oversight.
   */
  public Object getElementAt(JList<String> list, int index) {
    return list.getModel().getElementAt(index);
  }

  /**
   * Return the size of the given list.
   */
  public int getSize(JList<?> list) {
    return list.getModel().getSize();
  }

  /**
   * Return an array of strings that represents the list's contents.
   */
  public String[] getContents(JList<String> list) {
    ListModel<String> model = list.getModel();
    String[] values = new String[model.getSize()];
    for (int i = 0; i < values.length; i++) {
      values[i] = model.getElementAt(i);
    }
    return values;
  }

  /**
   * Select the given index. Equivalent to actionSelectRow(component, new JListLocation(index)).
   */
  public void actionSelectIndex(JList<?> list, int index) {
    actionSelectRow(list, new JListLocation(index));
  }

  /**
   * Select the first item in the list matching the given String representation of the item.<point>
   * Equivalent to actionSelectRow(component, new JListLocation(item)).
   */
  public void actionSelectItem(JList<String> list, String item) {
    actionSelectRow(list, new JListLocation(item));
  }

  /**
   * Select the first value in the list matching the given String representation of the
   * value.<point> Equivalent to actionSelectRow(component, new JListLocation(value)).
   */
  public void actionSelectValue(JList<String> list, String value) {
    actionSelectRow(list, new JListLocation(value));
  }

  /**
   * Select the given row.  Does nothing if the index is already selected.
   */
  public void actionSelectRow(JList<?> list, JListLocation location) {
    int index = location.getIndex(list);

    if (index < 0 || index >= list.getModel().getSize()) {
      String msg = Strings.get("tester.JList.invalid_index", new Object[] {index});
      throw new ActionFailedException(msg);
    }

    super.actionClick(list, location);
  }

  @Override
  public ComponentLocation parseLocation(String encoded) {
    return new JListLocation().parse(encoded);
  }

  @Override
  public ComponentLocation getLocation(Component component, Point point) {
    JList<String> list = (JList<String>) component;
    int index = list.locationToIndex(point);

    String value = valueToString(list, index);
    if (value != null) {
      return new JListLocation(value);
    } else if (index != -1) {
      return new JListLocation(index);
    }
    return new JListLocation(point);
  }
}
