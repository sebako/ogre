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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import de.bastisoft.util.swing.IconCache;
import de.bastisoft.util.swing.IconCacheException;
import de.bastisoft.util.swing.LabelText;

public class Resources {

    private static ResourceBundle strings;
    
    public static ResourceBundle strings() {
        if (strings == null)
            strings = ResourceBundle.getBundle("strings.ogregui");
        return strings;
    }
    
    public static String string(String key) {
        return strings().getString(key);
    }
    
    public static LabelText label(String key) {
        ResourceBundle strings = strings();
        String label = strings.getString(key);
        
        Integer mnemonic = null, mnemonicIndex = null;
        try {
            mnemonic = keyStroke(key + ".key").getKeyCode();
            mnemonicIndex = Integer.parseInt(strings.getString(key + ".index"));
        }
        catch (MissingResourceException | NumberFormatException e) {
            // mnemonic and/or mnemonicIndex remain null
        }
        
        return new LabelText(label, mnemonic, mnemonicIndex);
    }
    
    public static KeyStroke keyStroke(String key) {
        try {
            return KeyStroke.getKeyStroke(string(key));
        }
        catch (MissingResourceException e) {
            return null;
        }
    }
    
    public static String formatKeyStroke(KeyStroke keyStroke) {
        int mod = keyStroke.getModifiers();
        return (mod != 0 ? KeyEvent.getKeyModifiersText(mod) + "+" : "") + KeyEvent.getKeyText(keyStroke.getKeyCode());
    }
    
    private static IconCache icons;
    
    private static final Icon NO_ICON;
    
    static {
        BufferedImage empty = new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = empty.getGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 20, 20);
        
        NO_ICON = new ImageIcon(empty);
    }
    
    public static IconCache icons() {
        if (icons == null) {
            Map<String, String> names = new HashMap<>();
            names.put("add-server", "add-server.png");
            names.put("add",        "add.png");
            names.put("delete",     "delete.png");
            names.put("edit",       "edit.png");
            names.put("dir-match",  "silk/folder.png");
            names.put("file-match", "silk/page-modified.png");
            names.put("line-match", "fatcow/bullet_go.png");
            names.put("about",      "about.png");
            icons = new IconCache("/img/", names);
        }
        return icons;
    }
    
    public static Icon icon(String name) {
        try {
            return icons().getIcon(name);
        }
        catch (IconCacheException e) {
            return NO_ICON;
        }
    }
    
    public static Icon loadIcon(String name) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY);
        
        URL url = Resources.class.getResource(name);
        if (url != null) {
            try (InputStream in = url.openStream()) {
                img = ImageIO.read(in);
            }
            catch (IOException e) {
                // ...
            }
        }
        
        return new ImageIcon(img);
    }
    
}
