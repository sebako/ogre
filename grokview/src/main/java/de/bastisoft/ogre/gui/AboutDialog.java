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

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import de.bastisoft.util.swing.dialog.CloseDialog;
import de.bastisoft.util.swing.header.StatusHeader;

public class AboutDialog extends CloseDialog {
    
    private static final String RES_PREFIX       = "about.";
    
    private static final String RES_TEXT         = RES_PREFIX + "text";
    private static final String RES_DIALOG_TITLE = RES_PREFIX + "dialog.title";
    private static final String RES_CLOSE_BUTTON = RES_PREFIX + "close.button";
    private static final String RES_HEADLINE     = RES_PREFIX + "headline";
    private static final String RES_SUBTITLE     = RES_PREFIX + "subtitle";

    private String guiVersion;
    private String ogreVersion;
    
    public AboutDialog(Frame owner) {
        super(owner, Resources.string(RES_DIALOG_TITLE), true, Resources.label(RES_CLOSE_BUTTON));
        readVersions();
        setContent(makeWidgets());
        setResizable(false);
    }

    private JComponent makeWidgets() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        
        Icon icon = Resources.icon("about");
        String headline = MessageFormat.format(Resources.string(RES_HEADLINE), guiVersion);
        String subtitle = Resources.string(RES_SUBTITLE);
        StatusHeader header = new StatusHeader(headline, subtitle, icon, 400);
        
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

    private Component makeMainArea() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        
        String message = MessageFormat.format(
                Resources.string(RES_TEXT),
                ogreVersion,
                System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version"),
                Runtime.getRuntime().freeMemory());
        JLabel contentLabel = new JLabel(message);
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1;
        c.insets = new Insets(15, 15, 15, 15);
        panel.add(contentLabel, c);
        
        return panel;
    }
    
    private void readVersions() {
        guiVersion = readProperty("/de/bastisoft/ogre/gui/version.properties", "grokview.version");
        ogreVersion = readProperty("/de/bastisoft/ogre/version.properties", "ogre.version");
    }
    
    private static String readProperty(String path, String key) {
        String version = null;
        
        try (InputStream in = AboutDialog.class.getResourceAsStream(path)) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                version = p.getProperty(key);
            }
        }
        catch (IOException e) {
            // ...
        }
        
        if (version == null || version.startsWith("$"))
            version = "[unreleased]";
        
        return version;
    }

}
