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

// Fri Oct 25 18:07:43 EST 2004
//
// Written by Sean R. Owens, released to the public
// domain.  Share and enjoy.  http://darksleep.com/player

// A very simple custom dialog that takes a string as a parameter,
// displays it in a JLabel, along with two Jbuttons, one labeled Yes,
// and one labeled No, and waits for the user to click one of them.

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SampleDialog extends JDialog {

  private final JButton yesButton;
  private final JButton noButton;
  private boolean answer = false;

  public boolean getAnswer() {
    return answer;
  }

  public SampleDialog(JFrame frame, String myMessage) {
    super(frame);

    JPanel myPanel = new JPanel();
    getContentPane().add(myPanel);
    myPanel.add(new JLabel(myMessage));
    yesButton = new JButton("Yes");
    //   setVisible(false);
    //   setVisible(false);
    ActionListener actionListener =
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (yesButton == e.getSource()) {
              System.err.println("User chose yes.");
              answer = true;
              //   setVisible(false);
            } else if (noButton == e.getSource()) {
              System.err.println("User chose no.");
              answer = false;
              //   setVisible(false);
            }
          }
        };
    yesButton.addActionListener(actionListener);
    myPanel.add(yesButton);
    noButton = new JButton("No");
    noButton.addActionListener(actionListener);
    myPanel.add(noButton);
    setTitle("Question");
    pack();
    setVisible(true);
  }
}
