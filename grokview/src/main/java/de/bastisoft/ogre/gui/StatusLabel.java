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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.event.ProgressListener.Phase;

class StatusLabel extends JLabel {
    
    private static final String RUNNING_STRING = "Searching...";
    private static final String READY_STRING = "READY.";
    private static final String ERROR_STRING = "?ERROR";
    
    StatusLabel() {
        super(READY_STRING);
    }
    
    void queryStarted() {
        setText(RUNNING_STRING);
    }
    
    void progress(Phase phase, int current, int overall) {
        current++;
        int percent = current * 100 / overall;
        setText(String.format("%s - %d/%d (%d%%)", phase, current, overall, percent));
    }
    
    void queryFinshed(List<FileMatch> matches) {
        Set<String> dirset = new HashSet<>();
        int linecount = 0;
        for (FileMatch m : matches) {
            dirset.add(m.getDirectory());
            linecount += m.getLines().size();
        }
        setText(String.format("Directories: %d  Files: %d  Lines: %d", dirset.size(), matches.size(), linecount));
    }
    
    void queryAborted() {
        setText(ERROR_STRING);
    }
    
}