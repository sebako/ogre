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
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import de.bastisoft.ogre.FileMatch;
import de.bastisoft.ogre.FileMatch.Highlight;
import de.bastisoft.ogre.FileMatch.LineMatch;

class CellRenderer extends DefaultTreeCellRenderer {
    
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
        sb.append(lines);
        sb.append(" line");
        if (lines > 1)
            sb.append("s");
        
        if (match.abridged())
            sb.append(" &ndash; abridged");
        
        sb.append(")</font>");
        
        setText(sb.toString());
        setIcon(fileIcon);
    }
    
    private void displayLineMatch(LineMatch match) {
        int p = 0;
        while (p < match.line.length() - 1 && Character.isWhitespace(match.line.charAt(p)))
            p++;
        
        List<Highlight> hl = new ArrayList<>(match.highlights.size());
        for (Highlight h : match.highlights) {
            int begin = h.beginIndex;
            int end = h.endIndex;
            
            if (end > p) {
                if (begin < p)
                    begin = p;
                hl.add(new Highlight(begin, end));
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html><font color='#" + colorLineNumber + "'>");
        sb.append(match.lineNumber);
        sb.append(":</font> ");
        
        for (Highlight h : hl) {
            sb.append(htmlEncode(match.line.substring(p, h.beginIndex)));
            sb.append("<font color='#" + colorHighlight + "'><b>");
            sb.append(htmlEncode(match.line.substring(h.beginIndex, h.endIndex)));
            sb.append("</b></font>");
            p = h.endIndex;
        }
        
        sb.append(htmlEncode(match.line.substring(p)));
        
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