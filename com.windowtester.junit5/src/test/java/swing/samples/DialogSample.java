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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class DialogSample extends JDialog {

  private final JButton yesButton;
  private final JButton noButton;

  private boolean answer = false;

  public boolean getAnswer() {
    return answer;
  }

  ActionListener actionListener =
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (yesButton == e.getSource()) {
            answer = true;
            JOptionPane.showMessageDialog(
                yesButton.getParent(),
                "Eggs are not supposed to be green.",
                "Inane error",
                JOptionPane.ERROR_MESSAGE);
          } else if (noButton == e.getSource()) {
            System.err.println("User chose no.");
            answer = false;
          }
        }
      };

  public DialogSample(JFrame frame, String myMessage) {
    super(frame);

    setTitle("Question");

    JPanel myPanel = new JPanel();
    myPanel.add(new JLabel(myMessage));

    yesButton = new JButton("Yes");
    yesButton.addActionListener(actionListener);
    myPanel.add(yesButton);

    noButton = new JButton("No");
    noButton.addActionListener(actionListener);
    myPanel.add(noButton);

    getContentPane().add(myPanel);
    pack();
    setVisible(true);
  }
}
