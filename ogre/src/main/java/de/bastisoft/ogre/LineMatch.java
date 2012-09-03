package de.bastisoft.ogre;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineMatch {
    
    public static class Highlight {
        
        private int start, end;
        
        public Highlight(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        public int getStart() {
            return start;
        }
        
        public int getEnd() {
            return end;
        }
        
    }
    
    private int lineNumber;
    private String line;
    private WebLink link;
    private List<Highlight> highlights;
    
    LineMatch(int lineNumber, String line, WebLink link, List<Integer> positions) {
        if (positions.size() % 2 > 0)
            throw new IllegalArgumentException("positions.size() is odd");
        
        this.lineNumber = lineNumber;
        this.line = line;
        this.link = link;
        
        highlights = new ArrayList<>(positions.size() / 2);
        for (int i = 0; i < positions.size() / 2; i++)
            highlights.add(new Highlight(positions.get(2 * i), positions.get(2 * i + 1)));
        
        highlights = Collections.unmodifiableList(highlights);
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getLine() {
        return line;
    }
    
    public WebLink getLink() {
        return link;
    }
    
    public List<Highlight> getHighlights() {
        return highlights;
    }
    
}