package example;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Source code for Tutorial 2.
 */
public class LabeledList extends JPanel {
  private final JList list;
  private final JLabel label;

  public LabeledList(String[] initialContents) {
    setLayout(new BorderLayout());
    list = new JList(initialContents);
    add(list, BorderLayout.CENTER);
    label = new JLabel("Selected: ");
    add(label, BorderLayout.SOUTH);
    // Update the label whenever the list selection changes
    list.addListSelectionListener(
        new ListSelectionListener() {
          public void valueChanged(ListSelectionEvent lse) {
            label.setText("Selected: " + list.getSelectedValue());
          }
        });
  }
}
