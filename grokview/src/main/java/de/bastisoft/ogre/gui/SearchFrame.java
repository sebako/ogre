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

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.LineMatch;
import de.bastisoft.ogre.Scraper;
import de.bastisoft.ogre.SearchResult;
import de.bastisoft.ogre.WebLink;
import de.bastisoft.ogre.event.ResultReceiver;
import de.bastisoft.ogre.gui.Config.LookAndFeelSetting;
import de.bastisoft.ogre.gui.QueryInputs.Input;
import de.bastisoft.ogre.gui.tree.LinkHandler;
import de.bastisoft.ogre.gui.tree.ResultTree;
import de.bastisoft.util.swing.SwingUtils;

public class SearchFrame extends JFrame {
    
    private static final String PROGRAM_NAME = "GrokView";
    public static final String VERSION = "0.1";
    
    private static final String RES_PREFIX               = "main.";
    private static final String RES_SEARCH               = RES_PREFIX + "search";
    private static final String RES_SEARCH_SHORTCUT      = RES_PREFIX + "search.shortcut";
    private static final String RES_STOP                 = RES_PREFIX + "stop";
    private static final String RES_STOP_SHORTCUT        = RES_PREFIX + "stop.shortcut";
    private static final String RES_NO_SERVER_TITLE      = RES_PREFIX + "no.server.title";
    private static final String RES_NO_SERVER_MESSAGE    = RES_PREFIX + "no.server.message";
    private static final String RES_NO_INPUT_TITLE       = RES_PREFIX + "no.input.title";
    private static final String RES_NO_INPUT_MESSAGE     = RES_PREFIX + "no.input.message";
    private static final String RES_EMPTY_RESULT_TITLE   = RES_PREFIX + "empty.result.title";
    private static final String RES_EMPTY_RESULT_MESSAGE = RES_PREFIX + "empty.result.message";
    private static final String RES_BROWSE_ERROR_TITLE   = RES_PREFIX + "browse.error.title";
    private static final String RES_BROWSE_ERROR_LINK    = RES_PREFIX + "browse.error.link.message";
    private static final String RES_BROWSE_ERROR_LAUNCH  = RES_PREFIX + "browse.error.launch.message";
    
    private JSplitPane splitPane;
    private ServerChoicePanel serverChoicePanel;
    private QueryPanel queryPanel;
    private JButton searchButton;
    private JButton stopButton;
    private JButton aboutButton;
    private StatusBar statusBar;
    private ResultTree tree;
    
    private AboutDialog aboutDialog;
    private Desktop desktop;
    
    private boolean searchRunning;
    private Scraper scraper;
    private Config loadedConfig;
    private ServerSelection selection;
    
    public SearchFrame(Config config) {
        super(PROGRAM_NAME);
        
        if (Desktop.isDesktopSupported())
            desktop = Desktop.getDesktop();
        
        // Store config so that it can be used as a basis when saving the configuration on exit
        this.loadedConfig = config;
        
        selection = new ServerSelection();
        selection.setSelected(config.selectedServer);
        
        makeWidgets();
        makeActions();
        pack();
        
        if (config.frameState != null)
            applyState(config.frameState);
        else
            setLocationRelativeTo(null);
        
        serverChoicePanel.setServers(config.servers);
        
        List<Image> icons = new ArrayList<>();
        for (int size : new int[] {16, 32, 48})
            icons.add(Resources.image("gv-app-" + size + ".png"));
        setIconImages(icons);
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setVisible(true);
    }
    
    private void makeWidgets() {
        JPanel panel = new JPanel();
        setContentPane(panel);
        panel.setLayout(new GridBagLayout());
        
        tree = new ResultTree();
        tree.setExpanding(false);
        
        JScrollPane listScroller = new JScrollPane(tree);
        listScroller.setPreferredSize(new Dimension(600, 350));
        listScroller.setBorder(BorderFactory.createEtchedBorder());
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, makeSideBar(), listScroller);
        splitPane.setDividerSize(4);
        splitPane.setOneTouchExpandable(false);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.resetToPreferredSizes();
        
        statusBar = new StatusBar();
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1;
        c.insets = new Insets(4, 4, 4, 4);
        panel.add(splitPane, c);
        
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 4, 4, 4);
        panel.add(statusBar, c);
    }
    
    private JPanel makeSideBar() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new GridBagLayout());
        
        queryPanel = new QueryPanel(selection, loadedConfig.projectListLength);
        serverChoicePanel = new ServerChoicePanel(this, selection);
        
        searchButton = SwingUtils.makeButton(Resources.label(RES_SEARCH));
        KeyStroke shortcut = Resources.keyStroke(RES_SEARCH_SHORTCUT);
        searchButton.setToolTipText(searchButton.getText() +
                (shortcut != null ? " (" + Resources.formatKeyStroke(shortcut) + ")" : ""));
        
        stopButton = SwingUtils.makeButton(Resources.label(RES_STOP));
        shortcut = Resources.keyStroke(RES_STOP_SHORTCUT);
        stopButton.setToolTipText(stopButton.getText() +
                (shortcut != null ? " (" + Resources.formatKeyStroke(shortcut) + ")" : ""));
        stopButton.setEnabled(false);
        
        SwingUtils.equalizeButtons(searchButton, stopButton);
        
        aboutButton = new JButton("<html><font color='#0000C0'><u>About</u></font>");
        aboutButton.setBorder(BorderFactory.createEmptyBorder());
        aboutButton.setBorderPainted(false);
        aboutButton.setContentAreaFilled(false);
        aboutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.weightx = 1;
        c.insets = new Insets(5, 5, 0, 5);
        panel.add(serverChoicePanel, c);
        
        c.insets = new Insets(20, 5, 0, 5);
        panel.add(queryPanel, c);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(stopButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        buttonPanel.add(searchButton);
        
        panel.add(buttonPanel, c);
        
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LAST_LINE_END;
        c.insets = new Insets(20, 5, 5, 5);
        panel.add(aboutButton, c);
        
        return panel;
    }
    
    private void makeActions() {
        Action searchAction = new AbstractAction() {
            @Override    
            public void actionPerformed(ActionEvent e) {
                startSearch();
            }
        };
        
        Action stopAction = new AbstractAction() {
            @Override    
            public void actionPerformed(ActionEvent e) {
                stopSearch();
            }
        };
        
        searchButton.addActionListener(searchAction);
        stopButton.addActionListener(stopAction);
        
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        KeyStroke shortcut = Resources.keyStroke(RES_SEARCH_SHORTCUT);
        if (shortcut != null)
            inputMap.put(shortcut, "search");
        shortcut = Resources.keyStroke(RES_STOP_SHORTCUT);
        if (shortcut != null)
            inputMap.put(shortcut, "stop");
        
        ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put("search", searchAction);
        actionMap.put("stop", stopAction);
        
        tree.setFileRequestHandler(new LinkHandler() {
            @Override
            public void requested(LineMatch match) { openLink(match.getLink()); }
            
            @Override
            public void requested(FileMatch match) { openLink(match.getXrefLink()); }
        });
        
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (aboutDialog == null)
                    aboutDialog = new AboutDialog(SearchFrame.this);
                aboutDialog.setVisible(true);
            }
        });
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveConfig();
                System.exit(0);
            }
        });
    }
    
    private void startSearch() {
        if (searchRunning)
            return;
        
        Server server = selection.getSelected();
        
        if (server == null) {
            String message = Resources.string(RES_NO_SERVER_MESSAGE);
            String title = Resources.string(RES_NO_SERVER_TITLE);
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        boolean queryEmpty = true;
        for (Input input : Input.values())
            if (input != Input.PROJECT) {     // Project isn't really a query value
                String s = server.queryInputs.getInput(input);
                if (s != null && s.length() > 0) {
                    queryEmpty = false;
                    break;
                }
            }
        
        if (queryEmpty) {
            String message = Resources.string(RES_NO_INPUT_MESSAGE);
            String title = Resources.string(RES_NO_INPUT_TITLE);
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        setRunning(true);
        tree.reset();
        
        try {
            URL baseURL = new URL(server.serverSettings.baseURL);
            scraper = new Scraper(baseURL, getProxy(server));
            
            scraper.setPageLimit(server.serverSettings.limitPages ? server.serverSettings.pageLimit : Integer.MAX_VALUE);
            scraper.setFetchLines(server.serverSettings.fetchLines);
            scraper.setFetchLinesLast(server.serverSettings.fetchLinesLast);
            
            scraper.addProgressListener(statusBar);
            
            scraper.addResultReceiver(new ResultReceiver() {
                
                @Override
                public void newFileMatches(final Collection<FileMatch> newMatches) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tree.addFileMatches(newMatches);
                        }
                    });
                }
                
                @Override
                public void newLineMatches(final FileMatch amendedMatch) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tree.updateFileMatch(amendedMatch);
                        }
                    });
                }
                
            });
            
            statusBar.queryStarted();
            
            final QueryInputs inputs = selection.getSelected().queryInputs;
            Runnable runner = new Runnable() {
                public void run() {
                    try {
                        final SearchResult result = scraper.search(
                                inputs.getInput(Input.QUERY),
                                inputs.getInput(Input.DEFS),
                                inputs.getInput(Input.REFS),
                                inputs.getInput(Input.PATH),
                                inputs.getInput(Input.HIST),
                                inputs.getInput(Input.PROJECT));
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                finishQuery(result);
                            }
                        });
                    }
                    catch (final Exception e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                queryFailed(e);
                            }
                        });
                    }
                }
            };
            
            new Thread(runner).start();
        }
        catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid URL: " + e.getMessage(), "",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private Proxy getProxy(Server server) {
        if (server.serverSettings.proxyHost == null
                || server.serverSettings.proxyHost.length() == 0)
            return null;
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                server.serverSettings.proxyHost, server.serverSettings.proxyPort));
    }
    
    private void stopSearch() {
        if (searchRunning && scraper != null)
            scraper.abort();
    }
    
    private void finishQuery(SearchResult result) {
        if (result.files().size() == 0) {
            String title = Resources.string(RES_EMPTY_RESULT_TITLE);
            Object message = Resources.string(RES_EMPTY_RESULT_MESSAGE);
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
        }
        setRunning(false);
        statusBar.queryFinshed(result);
        tree.expandFirst();
    }
    
    private void queryFailed(Exception e) {
        setRunning(false);
        statusBar.queryAborted();
        JOptionPane.showMessageDialog(this, "Query aborted: " + e.getMessage(),
                "Error processing query", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
    
    private void setRunning(boolean running) {
        searchRunning = running;
        searchButton.setEnabled(!running);
        stopButton.setEnabled(running);
    }
    
    private void openLink(WebLink link) {
        if (link != null && desktop != null) {
            try {
                desktop.browse(link.url.toURI());
            }
            catch (URISyntaxException e) {
                String title = Resources.string(RES_BROWSE_ERROR_TITLE);
                String message = MessageFormat.format(Resources.string(RES_BROWSE_ERROR_LINK), e.getMessage());
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            }
            catch (IOException | UnsupportedOperationException | SecurityException e) {
                // So many things that could go wrong :(
                String title = Resources.string(RES_BROWSE_ERROR_TITLE);
                String message = MessageFormat.format(Resources.string(RES_BROWSE_ERROR_LAUNCH), e.getMessage());
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveConfig() {
        loadedConfig.servers = serverChoicePanel.getServers();
        loadedConfig.frameState = recordState();
        loadedConfig.selectedServer = selection.getSelected();
        Config.saveConfig(loadedConfig);
    }
    
    private FrameState recordState() {
        return new FrameState(
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                (getExtendedState() & MAXIMIZED_HORIZ) != 0,
                (getExtendedState() & MAXIMIZED_VERT) != 0,
                splitPane.getDividerLocation());
    }
    
    private void applyState(FrameState state) {
        setLocation(state.x, state.y);
        setSize(state.width, state.height);
        
        int es = getExtendedState();
        
        if (state.maximizedHorizontal)
            es |= MAXIMIZED_HORIZ;
        if (state.maximizedVertical)
            es |= MAXIMIZED_VERT;
        
        setExtendedState(es);
        
        if (state.splitPosition > 0)
            splitPane.setDividerLocation(state.splitPosition);
    }
    
    public static void main(String[] args) {
        Config config = Config.readConfig();
        
        if (config.lookAndFeelSetting != LookAndFeelSetting.DEFAULT) {
            try {
                if (config.lookAndFeelSetting == LookAndFeelSetting.CROSS_PLATFORM)
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                
                else if (config.lookAndFeelSetting == LookAndFeelSetting.SYSTEM)
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                else if (config.lookAndFeel != null) {
                    boolean found = false;
                    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                        if (config.lookAndFeel.equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            found = true;
                        }
                    
                    if (!found)
                        UIManager.setLookAndFeel(config.lookAndFeel);
                }
            }
            catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not set configured look and feel:\n\"" + config.lookAndFeel + "\"",
                        "Error setting look and feel",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        
        /* A somewhat ugly hack to fix widget fonts on Windows: In Windows versions 6 (Vista) and 7,
         * under non-classic themes, Swing application don't use the proper font for most widgets,
         * although they do use it on menus. (Typically it should be Segoe UI, but is Tahoma.)
         * 
         * To fix this, if we're running the WindowsLookAndFeel, we take the correct font from the
         * Menu.font default, and the incorrect font from Label.font. Then we replace the label
         * font with the menu font everywhere we find it. Generally this cannot fix all Windows
         * font quirks in Swing applications, but at least it seems to improve looks under most
         * circumstances and should never do particular harm. */
        
        if (UIManager.getLookAndFeel().getClass().getName().equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel")
                && System.getProperty("os.version").compareTo("6") > 0) {
            
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            
            Font menuFont = UIManager.getFont("Menu.font");
            Font labelFont = UIManager.getFont("Label.font");
            
            for (Object key : defaults.keySet())
                if (labelFont.equals(defaults.get(key)))
                    defaults.put(key, menuFont);
        }
        
        new SearchFrame(config);
    }
    
}
