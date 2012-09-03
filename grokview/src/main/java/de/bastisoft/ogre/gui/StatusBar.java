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

import java.awt.Dimension;
import java.awt.Font;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.SearchResult;
import de.bastisoft.ogre.event.ProgressListener;

class StatusBar extends JPanel implements ProgressListener {
    
    private static final String RES_PREFIX     = "status.";
    private static final String RES_INCOMPLETE = RES_PREFIX + "results.incomplete";
    private static final String RES_ABORTED    = RES_PREFIX + "search.aborted";
    private static final String RES_PAGE_LIMIT = RES_PREFIX + "pagelimit.triggered";
    
    private static class CountPanel extends JPanel {
        
        JLabel dirCountLabel, fileCountLabel, lineCountLabel;
        
        CountPanel() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            add(new JLabel("Directories:"));
            dirCountLabel = addLabel();
            
            add(Box.createRigidArea(new Dimension(8, 0)));
            add(new JLabel("Files:"));
            fileCountLabel = addLabel();
            
            add(Box.createRigidArea(new Dimension(8, 0)));
            add(new JLabel("Lines:"));
            lineCountLabel = addLabel();
        }
        
        JLabel addLabel() {
            JLabel label = new JLabel("0");
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            
            add(Box.createRigidArea(new Dimension(3, 0)));
            add(label);
            
            return label;
        }
        
        void setCounts(int dirCount, int fileCount, int lineCount) {
            dirCountLabel.setText(Integer.toString(dirCount));
            fileCountLabel.setText(Integer.toString(fileCount));
            lineCountLabel.setText(Integer.toString(lineCount));
        }
        
    }
    
    private Icon noticeIcon;
    private JLabel statusLabel;
    private CountPanel countPanel;
    
    StatusBar() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        add(statusLabel = new JLabel());
        add(Box.createGlue());
        add(countPanel = new CountPanel());
        
        noticeIcon = Resources.icon("notice");
    }
    
    void queryStarted() {
        statusLabel.setText(null);
        statusLabel.setIcon(null);
        countPanel.setCounts(0, 0, 0);
    }
    
    void queryFinshed(SearchResult result) {
        Set<String> dirset = new HashSet<>();
        int linecount = 0;
        Collection<FileMatch> files = result.files();
        for (FileMatch m : files) {
            dirset.add(m.getDirectory());
            linecount += m.getLines().size();
        }
        countPanel.setCounts(dirset.size(), files.size(), linecount);
        
        if (result.pageLimitTriggered() || result.aborted()) {
            String notice = Resources.string(RES_INCOMPLETE) + " ";
            
            if (result.aborted())
                notice += Resources.string(RES_ABORTED);
            
            else if (result.pageLimitTriggered())
                notice += (MessageFormat.format(Resources.string(RES_PAGE_LIMIT), result.fetchedPageCount()));
            
            statusLabel.setText(notice);
            statusLabel.setIcon(noticeIcon);
        }
        else {
            statusLabel.setText(null);
            statusLabel.setIcon(null);
        }
    }
    
    void queryAborted() {
        statusLabel.setText("?ERROR");
    }

    @Override
    public void progress(Phase phase, int current, int pending) {
        current++;
        int overall = current + pending;
        int percent = current * 100 / overall;
        statusLabel.setText(String.format("%s - %d/%d (%d%%)", phase, current, overall, percent));
    }
    
    @Override
    public void currentCounts(int dirCount, int fileCount, int lineCount) {
        countPanel.setCounts(dirCount, fileCount, lineCount);
    }
    
}