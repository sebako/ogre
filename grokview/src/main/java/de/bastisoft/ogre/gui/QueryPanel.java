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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import de.bastisoft.ogre.gui.QueryInputs.Input;
import de.bastisoft.ogre.gui.ServerSelection.ServerSelectionListener;
import de.bastisoft.util.swing.SwingUtils;

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
    
    private Map<Input, InputWidget> fields;
    private JComboBox<String> projectCombo;
    
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
        for (Input input : Input.values())
            fields.put(input, makeTextInput(input, c));
        
        serverSelected(selection.getSelected());
        selection.addListener(this);
    }
    
    private InputWidget makeTextInput(final Input input, GridBagConstraints c) {
        if (input != Input.PROJECT) {
            final JTextField field = new JTextField(20);
            JLabel label = SwingUtils.makeLabel(Resources.label(RES_PREFIX + input.resource), field);
            addWidget(field, label, c);
            
            field.getDocument().addDocumentListener(new Updater(input));
            
            return new InputWidget() {
                @Override
                public void setText(String text) {
                    field.setText(text);
                }
                
                @Override
                public String getText() {
                    return field.getText();
                }
                
                @Override
                public void setEnabled(boolean enabled) {
                    field.setEnabled(enabled);
                }
            };
        }
        
        else {
            projectCombo = new JComboBox<>();
            projectCombo.setEditable(true);
            JLabel label = SwingUtils.makeLabel(Resources.label(RES_PREFIX + input.resource), projectCombo);
            addWidget(projectCombo, label, c);
            
            projectCombo.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED)
                        update(input);
                }
            });
            
            return new InputWidget() {
                @Override
                public void setText(String text) {
//                    System.out.printf("setText(\"%s\")%n", text);
//                    projectCombo.setSelectedItem(text);
                }
                
                @Override
                public String getText() {
                    return (String) projectCombo.getSelectedItem();
                }
                
                @Override
                public void setEnabled(boolean enabled) {
                    projectCombo.setEnabled(enabled);
                }
            };
        }
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
            
            projectCombo.setModel(new ProjectComboModel(srv));
            
            autoUpdate = false;
        }
        
        for (InputWidget field : fields.values())
            field.setEnabled(server != null);
    }
    
    private static interface InputWidget {
        String getText();
        void setText(String text);
        void setEnabled(boolean enabled);
    }
    
    private static class ProjectComboModel implements ComboBoxModel<String> {

        private QueryInputs inputs;
        private List<String> projects;
        private List<ListDataListener> listeners = new CopyOnWriteArrayList<>();
        
        ProjectComboModel(Server srv) {
            inputs = srv.queryInputs;
            projects = new ArrayList<>(inputs.getProjects());
        }

        @Override
        public int getSize() {
            return projects.size();
        }

        @Override
        public String getElementAt(int index) {
            return projects.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }

        @Override
        public void setSelectedItem(Object item) {
            String project = (String) item;
            inputs.setInput(Input.PROJECT, project);
            if (!projects.contains(project)) {
                projects.add(0, project);
                for (ListDataListener l : listeners)
                    l.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, 0));
                
                if (projects.size() > 3) {
                    int last = projects.size() - 1;
                    projects.remove(last);
                    for (ListDataListener l : listeners)
                        l.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, last, last));
                }
                
                inputs.setProjects(projects);
            }
        }

        @Override
        public Object getSelectedItem() {
            return inputs.getInput(Input.PROJECT);
        }
        
    }
    
}
