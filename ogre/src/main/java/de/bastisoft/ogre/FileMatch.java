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

package de.bastisoft.ogre;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents a search hit for a single file, including information about the lines in the
 * file that specifically match the query.
 * 
 * <p>The directory and file name of an instance of this class are immutable. The list of
 * line matches is not: Upon initial retrieval, a file match object may be indicated to be
 * abridged. Depending on how the query is configured, it may or may not be be amended with
 * the full set of line matches at a later point in time.
 * 
 * @author Sebastian Koppehel
 */
public class FileMatch {

    private String directory;
    private String filename;
    private WebLink xrefLink;
    private SortedMap<Integer, LineMatch> lineMatches;
    private boolean abridged;
    private WebLink moreLink;
    
    FileMatch(String directory, String filename, WebLink xrefLink) {
        this.directory = directory;
        this.filename = filename;
        this.xrefLink = xrefLink;
        lineMatches = new TreeMap<>();
    }
    
    /**
     * Adds a new line to the line matches of the file match. Access to the line matches
     * is threadsafe.
     * 
     * @param line the line match
     * @return <code>true</code> is the line was not yet known
     */
    boolean addLine(LineMatch line) {
        synchronized (lineMatches) {
            if (lineMatches.get(line.getLineNumber()) == null) {
                lineMatches.put(line.getLineNumber(), line);
                return true;
            }
        }
        
        return false;
    }
    
    int merge(FileMatch other) {
        int added;
        
        synchronized (lineMatches) {
            added = lineMatches.size();
            lineMatches.putAll(other.lineMatches);
            added = lineMatches.size() - added;
        }
        
        /* How should we deal with the "abridged" status here? If either the existing
         * match or the one that was merged in did not have abridged status - i.e., had
         * all the line matches that exist in the file - then the merged match is also
         * not abridged.
         * 
         * Incidentally, the "More" link should really be the same too. If we didn't
         * have one before the merge, we don't need one. */
        
        abridged &= other.abridged;
        
        return added;
    }
    
    void setAbridged(WebLink moreLink) {
        this.moreLink = moreLink;
        abridged = true;
    }
    
    void setUnabridged() {
        abridged = false;
    }
    
    public String getDirectory() {
        return directory;
    }
    
    
    public String getFilename() {
        return filename;
    }
    
    public String getFullName() {
        return directory + filename;
    }
    
    /**
     * Returns the link to the file in the cross 
     * @return
     */
    public WebLink getXrefLink() {
        return xrefLink;
    }
    
    /**
     * Returns the lines in the file that have been found to match the query. If the
     * file hit is abridged, this list may not be complete, otherwise it is.
     * 
     * <p>This method is threadsafe, it may freely be called while the query is still
     * running. The returned list is a modifiable copy of the file matche's internal
     * representation of line hits.
     * 
     * @return lines with hits
     */
    public List<LineMatch> getLines() {
        synchronized (lineMatches) {
            return new ArrayList<>(lineMatches.values());
        }
    }
    
    /**
     * Returns <code>true</code> if this file hit is abgridged. Abridged means that
     * a number of line hits have been parsed, but the result page has indicated that
     * there are more lines with hits, and these have not yet been fetched.
     * 
     * @return <code>true</code> if this file it is abridged
     */
    public boolean abridged() {
        return abridged;
    }
    
    WebLink getMoreLink() {
        return moreLink;
    }
    
}
