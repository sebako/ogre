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

package de.bastisoft.ogre.gui.tree;

import static de.bastisoft.ogre.gui.Resources.icon;

import java.awt.Component;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.LineMatch;
import de.bastisoft.ogre.LineMatch.Highlight;
import de.bastisoft.ogre.gui.Resources;

class CellRenderer extends DefaultTreeCellRenderer {
    
    private static final String RES_PREFIX     = "tree.";
    private static final String RES_ZERO_LINES = RES_PREFIX + "lines.zero";
    private static final String RES_ONE_LINE   = RES_PREFIX + "lines.one";
    private static final String RES_MORE_LINES = RES_PREFIX + "lines.multiple";
    private static final String RES_ABRIDGED   = RES_PREFIX + "lines.abridged";
    
    private static final String COLOR_LINECOUNT     = "6070E0";
    private static final String COLOR_LINECOUNT_SEL = "C0D0FF";
    
    private static final String COLOR_LINENO        = "808080";
    private static final String COLOR_LINENO_SEL    = "D0D0D0";
    
    private static final String COLOR_HIGHLIGHT     = "70B900";
    private static final String COLOR_HIGHLIGHT_SEL = "C0E760";
    
    private Icon dirIcon;
    private Icon fileIcon;
    private Icon lineIcon;
    
    private String colorLineCount;
    private String colorLineNumber;
    private String colorHighlight;
    
    CellRenderer() {
        super();
        
        dirIcon = icon("dir-match");
        fileIcon = icon("file-match");
        lineIcon = icon("line-match");
    }
    
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        
        colorLineCount  = selected ? COLOR_LINECOUNT_SEL : COLOR_LINECOUNT;
        colorLineNumber = selected ? COLOR_LINENO_SEL    : COLOR_LINENO;
        colorHighlight  = selected ? COLOR_HIGHLIGHT_SEL : COLOR_HIGHLIGHT;
        
        if (value instanceof DirNode)
            displayDirectory((DirNode) value);
        
        else if (value instanceof FileNode)
            displayFile((FileNode) value);
        
        else if (value instanceof LineMatch)
            displayLineMatch((LineMatch) value);
        
        else
            setText("--");
        
        return this;
    }
    
    private void displayDirectory(DirNode dir) {
        setText(dir.name);
        setIcon(dirIcon);
    }
    
    private void displayFile(FileNode file) {
        FileMatch match = file.fileMatch;
        
        int lines = match.getLines().size();
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(htmlEncode(match.getFilename()));
        sb.append(" <font color='#" + colorLineCount + "'>(");
        
        if (lines == 0)
            sb.append(Resources.string(RES_ZERO_LINES));
        else if (lines == 1)
            sb.append(Resources.string(RES_ONE_LINE));
        else
            sb.append(MessageFormat.format(Resources.string(RES_MORE_LINES), lines));
        
        if (match.abridged()) {
            sb.append(" &ndash; ");
            sb.append(Resources.string(RES_ABRIDGED));
        }
        
        sb.append(")</font>");
        
        setText(sb.toString());
        setIcon(fileIcon);
    }
    
    private void displayLineMatch(LineMatch match) {
        String line = match.getLine();
        
        int p = 0;
        while (p < line.length() - 1 && Character.isWhitespace(line.charAt(p)))
            p++;
        
        List<Highlight> hl = new ArrayList<>(match.getHighlights().size());
        for (Highlight h : match.getHighlights()) {
            int begin = h.getStart();
            int end = h.getEnd();
            
            if (end > p) {
                if (begin < p)
                    begin = p;
                hl.add(new Highlight(begin, end));
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html><font color='#" + colorLineNumber + "'>");
        sb.append(match.getLineNumber());
        sb.append(":</font> ");
        
        for (Highlight h : hl) {
            sb.append(htmlEncode(line.substring(p, h.getStart())));
            sb.append("<font color='#" + colorHighlight + "'><b>");
            sb.append(htmlEncode(line.substring(h.getStart(), h.getEnd())));
            sb.append("</b></font>");
            p = h.getEnd();
        }
        
        sb.append(htmlEncode(line.substring(p)));
        
        setText(sb.toString());
        setIcon(lineIcon);
    }
    
    private static String htmlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
            case '<':
                sb.append("&lt;");
                break;
            
            case '>':
                sb.append("&gt;");
                break;
            
            case '&':
                sb.append("&amp;");
                break;
            
            default:
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
}