/*
 * Copyright 2012 Sebastian Koppehel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bastisoft.util.swing.dialog;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import de.bastisoft.util.swing.SwingUtils;

public abstract class StandardDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    
    private JComponent content;
    private JPanel buttonPanel;
    
    public StandardDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        
        JPanel panel = new JPanel();
        setContentPane(panel);
        panel.setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        panel.add(content = new JPanel(), c);
        
        c.weighty = 0;
        panel.add(buttonPanel = new JPanel(), c);
        
        pack();
        setLocationRelativeTo(owner);
    }
    
    protected void setButtons(JButton[] buttons) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        
        SwingUtils.equalizeButtons(buttons);
        
        panel.add(Box.createHorizontalGlue());
        for (int i = 0; i < buttons.length; i++) {
            panel.add(buttons[i]);
            if (i < buttons.length - 1)
                panel.add(Box.createRigidArea(new Dimension(5, 0)));
        }
        
        Container contentPane = getContentPane();
        contentPane.remove(buttonPanel);
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        contentPane.add(buttonPanel = panel, c);
        
        pack();
        setLocationRelativeTo(getOwner());
    }
    
    protected void setContent(JComponent content) {
        Container contentPane = getContentPane();
        contentPane.remove(this.content);
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        contentPane.add(this.content = content, c);
        
        pack();
        setLocationRelativeTo(getOwner());
    }
    
}
