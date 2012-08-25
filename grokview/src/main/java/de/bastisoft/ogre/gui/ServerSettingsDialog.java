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

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import de.bastisoft.util.swing.SwingUtils;
import de.bastisoft.util.swing.dialog.OkCancelDialog;
import de.bastisoft.util.swing.header.StatusHeader;
import de.bastisoft.util.swing.header.StatusMessage;
import de.bastisoft.util.swing.header.StatusMessage.Severity;

public class ServerSettingsDialog extends OkCancelDialog {

    private static final String RES_PREFIX           = "server.settings.";
    
    private static final String RES_TITLE            = RES_PREFIX + "dialog.title";
    private static final String RES_HEADER           = RES_PREFIX + "dialog.header";
    private static final String RES_NAME             = RES_PREFIX + "name";
    private static final String RES_BASEURL          = RES_PREFIX + "base.url";
    private static final String RES_PROXY_HOST       = RES_PREFIX + "proxy.host";
    private static final String RES_PROXY_PORT       = RES_PREFIX + "proxy.port";
    private static final String RES_FETCH_LINBS      = RES_PREFIX + "fetch.lines";
    private static final String RES_FETCH_LINBS_LAST = RES_PREFIX + "fetch.lines.last";
    private static final String RES_OK_BUTTON        = RES_PREFIX + "ok.button";
    private static final String RES_CANCEL_BUTTON    = RES_PREFIX + "cancel.button";
    
    private static final String RES_STATUS_FINE         = RES_PREFIX + "status.fine";
    private static final String RES_STATUS_NO_NAME      = RES_PREFIX + "status.no.name";
    private static final String RES_STATUS_NO_URL       = RES_PREFIX + "status.no.url";
    private static final String RES_STATUS_INVALID_URL  = RES_PREFIX + "status.invalid.url";
    private static final String RES_STATUS_NO_PORT      = RES_PREFIX + "status.no.port";
    private static final String RES_STATUS_INVALID_PORT = RES_PREFIX + "status.invalid.port";
    private static final String RES_STATUS_PORT_RANGE   = RES_PREFIX + "status.port.range";
    
    private JTextField nameField;
    private JTextField urlField;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JCheckBox fetchLinesCheck;
    private JCheckBox fetchLinesLastCheck;
    
    private StatusHeader header;
    
    ServerSettingsDialog(Frame owner) {
        super(
                owner,
                Resources.string(RES_TITLE),
                true,
                Resources.label(RES_OK_BUTTON),
                Resources.label(RES_CANCEL_BUTTON));
        
        setContent(makeWidgets());
        makeActions();
        
        setResizable(false);
    }
    
    private JPanel makeWidgets() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        
        Icon headerIcon = Resources.icon("add-server");
        header = new StatusHeader(
                Resources.string(RES_HEADER),
                Resources.string(RES_STATUS_FINE),
                headerIcon, 400);
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.weightx = 1;
        panel.add(header, c);
        panel.add(new JSeparator(), c);
        c.weighty = 1;
        panel.add(makeMainArea(), c);
        c.weighty = 0;
        panel.add(new JSeparator(), c);
        
        return panel;
    }
    
    private JPanel makeMainArea() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        panel.setLayout(new GridBagLayout());
        
        LabelFieldPair namePair = new LabelFieldPair(RES_NAME, 30);
        nameField = namePair.field;
        
        LabelFieldPair urlPair = new LabelFieldPair(RES_BASEURL, 30);
        urlField = urlPair.field;
        
        LabelFieldPair proxyHostPair = new LabelFieldPair(RES_PROXY_HOST, 20);
        proxyHostField = proxyHostPair.field;
        
        LabelFieldPair proxyPortPair = new LabelFieldPair(RES_PROXY_PORT, 4);
        proxyPortField = proxyPortPair.field;
        proxyPortField.setDocument(new PlainDocument() {
           @Override
           public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
               if (str == null || getLength() + str.length() < 6)
                   super.insertString(offs, str, a);
           }
        });
        
        fetchLinesCheck = new JCheckBox();
        SwingUtils.setText(fetchLinesCheck, Resources.label(RES_FETCH_LINBS));
        fetchLinesLastCheck = new JCheckBox();
        SwingUtils.setText(fetchLinesLastCheck, Resources.label(RES_FETCH_LINBS_LAST));
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.fill = GridBagConstraints.VERTICAL;
        panel.add(namePair.label, c);
        
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridwidth = 3;
        c.insets = new Insets(0, 10, 0, 0);
        panel.add(namePair.field, c);
        
        c.gridy++;
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(20, 0, 0, 0);
        panel.add(urlPair.label, c);
        
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridwidth = 3;
        c.insets = new Insets(20, 10, 0, 0);
        panel.add(urlPair.field, c);
        
        c.gridy++;
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(10, 0, 0, 0);
        panel.add(proxyHostPair.label, c);
        
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.insets = new Insets(10, 10, 0, 0);
        panel.add(proxyHostPair.field, c);
        
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0;
        c.insets = new Insets(10, 10, 0, 0);
        panel.add(proxyPortPair.label, c);
        
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 0, 0);
        panel.add(proxyPortPair.field, c);
        
        c.gridy++;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(15, 10, 0, 0);
        panel.add(fetchLinesCheck, c);
        
        c.gridy++;
        c.insets = new Insets(3, 10, 0, 0);
        panel.add(fetchLinesLastCheck, c);
        
        return panel;
    }
    
    private void makeActions() {
        DocumentListener docListener = new DocumentListener() {
            @Override public void changedUpdate(DocumentEvent e) {}
            @Override public void insertUpdate(DocumentEvent e) { updateButton(); }
            @Override public void removeUpdate(DocumentEvent e) { updateButton(); }
        };
        
        nameField.getDocument().addDocumentListener(docListener);
        urlField.getDocument().addDocumentListener(docListener);
        proxyHostField.getDocument().addDocumentListener(docListener);
        proxyPortField.getDocument().addDocumentListener(docListener);
        
        fetchLinesCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCheck();
            }
        });
    }
    
    private void updateCheck() {
        fetchLinesLastCheck.setEnabled(fetchLinesCheck.isSelected());
    }
    
    private static final int MSGID_NO_NAME      = 0;
    private static final int MSGID_NO_URL       = 1;
    private static final int MSGID_INVALID_URL  = 2;
    private static final int MSGID_NO_PORT      = 3;
    private static final int MSGID_INVALID_PORT = 4;
    private static final int MSGID_PORT_RANGE   = 5;
    
    private static final StatusMessage MSG_NO_NAME      = new StatusMessage(MSGID_NO_NAME,      Severity.ERROR, Resources.string(RES_STATUS_NO_NAME));
    private static final StatusMessage MSG_NO_URL       = new StatusMessage(MSGID_NO_URL,       Severity.ERROR, Resources.string(RES_STATUS_NO_URL));
    private static final StatusMessage MSG_INVALID_URL  = new StatusMessage(MSGID_INVALID_URL,  Severity.ERROR, Resources.string(RES_STATUS_INVALID_URL));
    private static final StatusMessage MSG_NO_PORT      = new StatusMessage(MSGID_NO_PORT,      Severity.ERROR, Resources.string(RES_STATUS_NO_PORT));
    private static final StatusMessage MSG_INVALID_PORT = new StatusMessage(MSGID_INVALID_PORT, Severity.ERROR, Resources.string(RES_STATUS_INVALID_PORT));
    private static final StatusMessage MSG_PORT_RANGE   = new StatusMessage(MSGID_PORT_RANGE,   Severity.ERROR, Resources.string(RES_STATUS_PORT_RANGE));
    
    @Override
    protected boolean ok() {
        if (nameField.getText().length() == 0) {
            header.addMessage(MSG_NO_NAME);
            return false;
        }
        else
            header.removeMessage(MSGID_NO_NAME);
        
        if (urlField.getText().length() == 0) {
            header.addMessage(MSG_NO_URL);
            return false;
        }
        else
            header.removeMessage(MSGID_NO_URL);
        
        try {
            new URL(urlField.getText());
            header.removeMessage(MSGID_INVALID_URL);
        }
        catch (MalformedURLException e) {
            header.addMessage(MSG_INVALID_URL);
            return false;
        }
        
        if (proxyHostField.getText().length() > 0 && proxyPortField.getText().length() == 0) {
            header.addMessage(MSG_NO_PORT);
            return false;
        }
        else
            header.removeMessage(MSGID_NO_PORT);
        
        if (proxyPortField.getText().length() > 0) {
            try {
                int port = Integer.parseInt(proxyPortField.getText());
                header.removeMessage(MSGID_INVALID_PORT);
                if (port < 0 || port > 65535) {
                    header.addMessage(MSG_PORT_RANGE);
                    return false;
                }
                else
                    header.removeMessage(MSGID_PORT_RANGE);
            }
            catch (NumberFormatException e) {
                header.addMessage(MSG_INVALID_PORT);
                return false;
            }
        }
        else {
            header.removeMessage(MSGID_INVALID_PORT);
            header.removeMessage(MSGID_PORT_RANGE);
        }
        
        return true;
    }
    
    boolean run(ServerSettings settings) {
        nameField.setText(settings.name);
        urlField.setText(settings.baseURL);
        proxyHostField.setText(settings.proxyHost);
        proxyPortField.setText(Integer.toString(settings.proxyPort));
        fetchLinesCheck.setSelected(settings.fetchLines);
        fetchLinesLastCheck.setSelected(settings.fetchLinesLast);
        
        updateCheck();
        
        return run();
    }
    
    ServerSettings getServerSettings() {
        return new ServerSettings(
                nameField.getText(),
                urlField.getText(),
                proxyHostField.getText(),
                Integer.parseInt(proxyPortField.getText()),
                fetchLinesCheck.isSelected(),
                fetchLinesLastCheck.isSelected());
    }

}
