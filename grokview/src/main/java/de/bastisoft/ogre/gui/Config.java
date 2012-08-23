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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.UIManager;

import de.bastisoft.ogre.gui.QueryInputs.Input;

class Config {

    static enum LookAndFeelSetting {
        
        DEFAULT         ("<default>"),
        CROSS_PLATFORM  ("<cross-platform>"),
        SYSTEM          ("<system>"),
        CLASS           (null);
        
        final String keyword;
        private LookAndFeelSetting(String keyword) { this.keyword = keyword; }
        
    };
    
    private static final String FILENAME_HOMEDIR     = ".grokview.properties";
    private static final String FILENAME_CLASSPATH   = "state.properties";
    
    
    // Server settings
    
    private static final String PREFIX_SERVER        = "server";
    
    private static final String KEY_NAME             = "name";
    private static final String KEY_BASE_URL         = "base.url";
    private static final String KEY_PROXY_HOST       = "proxy.host";
    private static final String KEY_PROXY_PORT       = "proxy.port";
    private static final String KEY_FETCH_LINES      = "fetch.lines";
    private static final String KEY_FETCH_LINES_LAST = "fetch.lines.last";
    
    
    // Query inputs
    
    private static final String PREFIX_INPUT         = "input";
    private static final Map<Input, String> INPUT_KEYS;
    
    static {
        INPUT_KEYS = new HashMap<>();
        
        INPUT_KEYS.put(Input.QUERY,   "query");
        INPUT_KEYS.put(Input.DEFS,    "defs");
        INPUT_KEYS.put(Input.REFS,    "refs");
        INPUT_KEYS.put(Input.PATH,    "path");
        INPUT_KEYS.put(Input.HIST,    "hist");
        INPUT_KEYS.put(Input.PROJECT, "project");
    }
    
    
    // Frame state
    
    private static final String PREFIX_FRAME         = "frame";
    
    private static final String KEY_FRAME_X          = "x";
    private static final String KEY_FRAME_Y          = "y";
    private static final String KEY_FRAME_WIDTH      = "width";
    private static final String KEY_FRAME_HEIGHT     = "height";
    private static final String KEY_MAXIMIZED_HORIZ  = "maximized.horizontal";
    private static final String KEY_MAXIMIZED_VERT   = "maximized.vertical";
    private static final String KEY_SPLIT_POSITION   = "split.position";
    
    
    // Other settings
    
    private static final String KEY_LOOK_AND_FEEL    = "look.and.feel";
    
    
    List<Server> sites;
    FrameState frameState;
    LookAndFeelSetting lookAndFeelSetting;
    String lookAndFeel;
    
    Config() {
        sites = new ArrayList<>();
        sites.add(new Server(new ServerSettings("Default", "", "", 8080, true, true), new QueryInputs()));
        frameState = null;
        
        lookAndFeelSetting = LookAndFeelSetting.DEFAULT;
        if (UIManager.getSystemLookAndFeelClassName().endsWith("windows.WindowsLookAndFeel"))
            lookAndFeelSetting = LookAndFeelSetting.SYSTEM;
        lookAndFeel = null;
    }
    
    private static Config read(PropertyMap props) {
        Config c = new Config();
        
        c.sites.clear();
        for (PropertyMap siteMap : props.submaps(PREFIX_SERVER))
            c.sites.add(new Server(readServerSettings(siteMap), readInputs(siteMap.submap(PREFIX_INPUT))));
        
        PropertyMap guiProps = props.submap(PREFIX_FRAME);
        FrameState frameState = readFrameState(guiProps);
        if (frameState != null)
            c.frameState = frameState;
        
        c.lookAndFeelSetting = LookAndFeelSetting.CLASS;
        c.lookAndFeel = props.get(KEY_LOOK_AND_FEEL, LookAndFeelSetting.DEFAULT.keyword);
        
        for (LookAndFeelSetting setting : LookAndFeelSetting.values())
            if (setting.keyword != null && setting.keyword.equalsIgnoreCase(c.lookAndFeel)) {
                c.lookAndFeelSetting = setting;
                break;
            }
        
        return c;
    }
    
    private static ServerSettings readServerSettings(PropertyMap props) {
        String name = props.get(KEY_NAME, "");
        String baseURL = props.get(KEY_BASE_URL, "");
        String proxyHost = props.get(KEY_PROXY_HOST, "");
        int proxyPort = props.getInt(KEY_PROXY_PORT, 8080);
        boolean fetchLines = props.getBool(KEY_FETCH_LINES, true);
        boolean fetchLinesLast = props.getBool(KEY_FETCH_LINES_LAST, true);
        
        return new ServerSettings(name, baseURL, proxyHost, proxyPort, fetchLines, fetchLinesLast);
    }
    
    private static QueryInputs readInputs(PropertyMap props) {
        QueryInputs inputs = new QueryInputs();
        
        for (Input input : Input.values()) {
            String key = INPUT_KEYS.get(input);
            if (key != null)
                inputs.setInput(input, props.get(key));
        }
        
        return inputs;
    }
    
    private static FrameState readFrameState(PropertyMap props) {
        int x = props.getInt(KEY_FRAME_X, -1);
        int y = props.getInt(KEY_FRAME_Y, -1);
        int width = props.getInt(KEY_FRAME_WIDTH, 0);
        int height = props.getInt(KEY_FRAME_HEIGHT, 0);
        boolean maxHoriz = props.getBool(KEY_MAXIMIZED_HORIZ, false);
        boolean maxVert = props.getBool(KEY_MAXIMIZED_VERT, false);
        int splitPosition = props.getInt(KEY_SPLIT_POSITION, -1);
        
        if (x >= 0 && y >= 0 && width > 0 && height > 0)
            return new FrameState(x, y, width, height, maxHoriz, maxVert, splitPosition);
        
        return null;
    }
    
    private PropertyMap write() {
        PropertyMap props = new PropertyMap();
        
        for (int i = 0; i < sites.size(); i++) {
            Server site = sites.get(i);
            PropertyMap siteProps = new PropertyMap();
            
            siteProps.set(KEY_NAME, site.serverSettings.name);
            siteProps.set(KEY_BASE_URL, site.serverSettings.baseURL);
            siteProps.set(KEY_PROXY_HOST, site.serverSettings.proxyHost);
            siteProps.set(KEY_PROXY_PORT, Integer.toString(site.serverSettings.proxyPort));
            
            siteProps.set(KEY_FETCH_LINES, site.serverSettings.fetchLines);
            siteProps.set(KEY_FETCH_LINES_LAST, site.serverSettings.fetchLinesLast);
            
            PropertyMap inputProps = new PropertyMap();
            for (Input input : Input.values()) {
                String key = INPUT_KEYS.get(input);
                if (key != null)
                    inputProps.set(key, site.queryInputs.getInput(input));
            }
            
            siteProps.add(inputProps, PREFIX_INPUT);
            props.addIndexed(siteProps, PREFIX_SERVER, i);
        }
        
        PropertyMap guiProps = new PropertyMap();
        guiProps.set(KEY_FRAME_X, frameState.x);
        guiProps.set(KEY_FRAME_Y, frameState.y);
        guiProps.set(KEY_FRAME_WIDTH, frameState.width);
        guiProps.set(KEY_FRAME_HEIGHT, frameState.height);
        guiProps.set(KEY_MAXIMIZED_HORIZ, frameState.maximizedHorizontal);
        guiProps.set(KEY_MAXIMIZED_VERT, frameState.maximizedVertical);
        guiProps.set(KEY_SPLIT_POSITION, frameState.splitPosition);
        props.add(guiProps, PREFIX_FRAME);
        
        switch (lookAndFeelSetting) {
        case DEFAULT:
            props.set(KEY_LOOK_AND_FEEL, "<default>");
            break;
        case SYSTEM:
            props.set(KEY_LOOK_AND_FEEL, "<system>");
            break;
        default:
            props.set(KEY_LOOK_AND_FEEL, lookAndFeel);
        }
        
        return props;
    }
    
    static Config readConfig() {
        Config config = loadFromHomeDir();
        if (config != null)
            return config;
        
        config = loadFromClasspath();
        if (config != null)
            return config;
        
        return new Config();
    }
    
    private static Config loadFromHomeDir() {
        try {
            return read(PropertyMap.load(getConfigFile()));
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static Config loadFromClasspath() {
        try (InputStream stream = Config.class.getResourceAsStream(FILENAME_CLASSPATH)) {
            if (stream != null)
                return read(PropertyMap.load(stream));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    static void saveConfig(Config config) {
        PropertyMap props = config.write();
        try {
            props.save(getConfigFile());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path getConfigFile() {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        return homeDir.resolve(FILENAME_HOMEDIR);
    }

}
