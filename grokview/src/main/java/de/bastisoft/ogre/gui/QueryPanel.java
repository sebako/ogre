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

package de.bastisoft.ogre.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.bastisoft.ogre.gui.QueryInputs.Input;
import de.bastisoft.ogre.gui.ServerSelection.ServerSelectionListener;

class QueryPanel extends JPanel implements ServerSelectionListener {

    private static final String RES_PREFIX = "query.input.";
    
    private class Updater implements DocumentListener {
        
        private Input input;
        
        Updater(Input input) {
            this.input = input;
        }
        
        @Override
        public void removeUpdate(DocumentEvent e) {
            update(input);
        }
        
        @Override
        public void insertUpdate(DocumentEvent e) {
            update(input);
        }
        
        @Override public void changedUpdate(DocumentEvent e) {}
    }
    
    private Map<Input, JTextField> fields;
    
    private ServerSelection selection;
    private boolean autoUpdate;
    
    QueryPanel(ServerSelection selection) {
        this.selection = selection;
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.weightx = c.weighty = 1;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        
        fields = new HashMap<>();
        for (Input input : Input.values()) {
            LabelFieldPair pair = new LabelFieldPair(RES_PREFIX + input.resource, 20);
            addField(pair, c);
            pair.field.getDocument().addDocumentListener(new Updater(input));
            
            fields.put(input, pair.field);
        }
        
        serverSelected(selection.getSelected());
        selection.addListener(this);
    }
    
    private JTextField addField(LabelFieldPair pair, GridBagConstraints c) {
        addWidget(pair.field, pair.label, c);
        return pair.field;
    }
    
    private void addWidget(JComponent component, JLabel label, GridBagConstraints c) {
        int vdist = c.gridy == 0 ? 0 : 7;
        
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(vdist, 0, 0, 0);
        add(label, c);
        
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 0, 0, 0);
        add(component, c);
        
        c.gridy++;
    }
    
    private void update(Input input) {
        Server srv;
        if (autoUpdate || (srv = selection.getSelected()) == null)
            return;
        
        String text = fields.get(input).getText();
        if (text.length() == 0)
            text = null;
        
        srv.queryInputs.setInput(input, text);
    }
    
    @Override
    public void serverSelected(Server server) {
        Server srv = selection.getSelected();
        if (srv != null) {
            autoUpdate = true;
            
            for (Input input : Input.values())
                fields.get(input).setText(srv.queryInputs.getInput(input));
            
            autoUpdate = false;
        }
        
        for (JTextField field : fields.values())
            field.setEnabled(server != null);
    }
    
}
