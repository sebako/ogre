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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
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
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.FileMatch.LineMatch;
import de.bastisoft.ogre.Scraper;
import de.bastisoft.ogre.event.ProgressListener;
import de.bastisoft.ogre.event.ResultReceiver;
import de.bastisoft.ogre.gui.Config.LookAndFeelSetting;
import de.bastisoft.ogre.gui.QueryInputs.Input;
import de.bastisoft.ogre.gui.tree.LinkHandler;
import de.bastisoft.ogre.gui.tree.ResultTree;

public class SearchFrame extends JFrame {
	
    private static final String PROGRAM_NAME = "GrokView";
    public static final String VERSION = "0.1";
    
    private static final String RES_PREFIX               = "main.";
    private static final String RES_SEARCH               = RES_PREFIX + "search";
    private static final String RES_SEARCH_SHORTCUT      = RES_PREFIX + "search.shurtcut";
    private static final String RES_NO_SERVER_TITLE      = RES_PREFIX + "no.server.title";
    private static final String RES_NO_SERVER_MESSAGE    = RES_PREFIX + "no.server.message";
    private static final String RES_NO_INPUT_TITLE       = RES_PREFIX + "no.input.title";
    private static final String RES_NO_INPUT_MESSAGE     = RES_PREFIX + "no.input.message";
    private static final String RES_EMPTY_RESULT_TITLE   = RES_PREFIX + "empty.result.title";
    private static final String RES_EMPTY_RESULT_MESSAGE = RES_PREFIX + "empty.result.message";
    
    private JSplitPane splitPane;
	private ServerChoicePanel serverChoicePanel;
	private QueryPanel queryPanel;
	private JButton searchButton;
	private JButton aboutButton;
	private StatusLabel statusLabel;
	private ResultTree tree;
	
	private AboutDialog aboutDialog;
	private Desktop desktop;
	
	private boolean searchRunning;
	private Config loadedConfig;
	private ServerSelection selection;
	
	public SearchFrame(Config config) {
		super(PROGRAM_NAME);
		
		if (Desktop.isDesktopSupported())
			desktop = Desktop.getDesktop();
		
		selection = new ServerSelection();
		
		makeWidgets();
		makeActions();
		pack();
		
        if (config.frameState != null)
			applyState(config.frameState);
		else
			setLocationRelativeTo(null);
		
        serverChoicePanel.setServers(config.sites);
        
		// Store config so that it can be used as a basis when saving the configuration on exit
		this.loadedConfig = config;
		
		List<Image> icons = new ArrayList<>();
		for (int size : new int[] {16, 32, 48, 128})
		    icons.add(Resources.image("tango/edit-find_" + size + ".png"));
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
		
		statusLabel = new StatusLabel();
		
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
		panel.add(statusLabel, c);
	}
	
	private JPanel makeSideBar() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEtchedBorder());
		panel.setLayout(new GridBagLayout());
		
        queryPanel = new QueryPanel(selection);
		serverChoicePanel = new ServerChoicePanel(selection);
		
		searchButton = new JButton(Resources.string(RES_SEARCH));
	    KeyStroke shortcut = Resources.keyStroke(RES_SEARCH_SHORTCUT);
		searchButton.setToolTipText(searchButton.getText() +
		        (shortcut != null ? " (" + Resources.formatKeyStroke(shortcut) + ")" : ""));
		
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
		
		c.insets = new Insets(20, 5, 20, 5);
		panel.add(queryPanel, c);
		
        c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		c.insets = new Insets(0, 0, 5, 5);
		panel.add(searchButton, c);
		
		c.weighty = 0;
		c.anchor = GridBagConstraints.LAST_LINE_END;
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
		
		searchButton.addActionListener(searchAction);
		
		InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		KeyStroke shortcut = Resources.keyStroke(RES_SEARCH_SHORTCUT);
		if (shortcut != null)
		    inputMap.put(shortcut, "search");
		
		ActionMap actionMap = getRootPane().getActionMap();
		actionMap.put("search", searchAction);
		
		tree.setFileRequestHandler(new LinkHandler() {
			@Override
			public void requested(LineMatch match) {
				if (desktop != null) {
					try {
						desktop.browse(match.link.url.toURI());
					}
					catch (IOException | URISyntaxException e) {
						JOptionPane.showMessageDialog(SearchFrame.this,
								"Invalid URL: " + e.getMessage(),
								"Error opening file link",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
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
		
		searchRunning = true;
		searchButton.setEnabled(false);
		tree.reset();
		
		try {
			URL baseURL = new URL(server.serverSettings.baseURL);
			final Scraper scraper = new Scraper(baseURL, getProxy(server));
			scraper.setFetchLines(server.serverSettings.fetchLines);
			scraper.setFetchLinesLast(server.serverSettings.fetchLinesLast);
			
			scraper.addProgressListener(new ProgressListener() {
				
				@Override
				public void progress(final Phase phase, final int current,
						final int overall) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							statusLabel.progress(phase, current, overall);
						}
					});
				}
				
			});
			
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
			
			statusLabel.queryStarted();
			
			final QueryInputs inputs = selection.getSelected().queryInputs;
			Runnable runner = new Runnable() {
				public void run() {
					try {
						final List<FileMatch> results = scraper.search(
								inputs.getInput(Input.QUERY),
								inputs.getInput(Input.DEFS),
                                inputs.getInput(Input.REFS),
                                inputs.getInput(Input.PATH),
                                inputs.getInput(Input.PROJECT));
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								finishQuery(results);
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
	
	private void finishQuery(List<FileMatch> matches) {
	    if (matches.size() == 0) {
	        String title = Resources.string(RES_EMPTY_RESULT_TITLE);
	        Object message = Resources.string(RES_EMPTY_RESULT_MESSAGE);
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
	    }
		searchRunning = false;
		searchButton.setEnabled(true);
		statusLabel.queryFinshed(matches);
		tree.expandFirst();
	}
	
	private void queryFailed(Exception e) {
		searchRunning = false;
		searchButton.setEnabled(true);
		statusLabel.queryAborted();
		JOptionPane.showMessageDialog(this, "Query aborted: " + e.getMessage(),
				"Error processing query", JOptionPane.ERROR_MESSAGE);
		e.printStackTrace();
	}
	
	private void saveConfig() {
		loadedConfig.sites = serverChoicePanel.getServers();
		loadedConfig.frameState = recordState();
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
		
		new SearchFrame(config);
	}
	
}
