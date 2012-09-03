package de.bastisoft.ogre;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchResult {

    private List<FileMatch> matches;
    private int nextAbridged;
    
    private List<WebLink> resultPages;
    private int nextPage;
    
    private boolean limitTriggered;
    private boolean aborted;
    
    private Set<String> dirNames;
    private int lineCount;
    
    
    
    // Public API
    
    public Collection<FileMatch> files() {
        return Collections.unmodifiableCollection(matches);
    }
    
    public boolean pageLimitTriggered() {
        return limitTriggered;
    }
    
    public boolean aborted() {
        return aborted;
    }
    
    public int fetchedPageCount() {
        return nextPage;
    }
    
    public int unfetchedPageCount() {
        return resultPages.size() - nextPage;
    }
    
    public int abridgedFileCount() {
        int n = 0;
        
        // advance the pointer if possible
        nextAbridgedFile();
        
        for (int i = nextAbridged; i < matches.size(); i++)
            if (matches.get(i).abridged())
                n++;
        
        return n;
    }
    
    public int dirCount() {
        return dirNames.size();
    }
    
    public int fileCount() {
        return matches.size();
    }
    
    public int lineCount() {
        return lineCount;
    }
    
    
    // OGRE internal API
    
    SearchResult(WebLink startURL) {
        matches = new ArrayList<>();
        resultPages = new ArrayList<>();
        resultPages.add(startURL);
        dirNames = new HashSet<>();
    }
    
    /**
     * Returns the next result page that should be retrieved. This method does not advance
     * the internal pointer, i.e. repeated calls to this method will return the same link
     * unless {@link #notifyFetched} is called in between.
     * 
     * @return the next result page that should be retrieved or <code>null</code> if there
     *          are no further result pages
     */
    WebLink nextPage() {
        return nextPage < resultPages.size()
                ? resultPages.get(nextPage)
                : null;
    }
    
    /**
     * Returns the next abridged file match that should be amended by fetching additional
     * line matches.
     * 
     * @return the next abridged file match, or <code>null</code> if there are no more
     *          abridged file matches
     */
    FileMatch nextAbridgedFile() {
        while (nextAbridged < matches.size() && !matches.get(nextAbridged).abridged())
            nextAbridged++;
        
        return nextAbridged < matches.size()
                ? matches.get(nextAbridged)
                : null;
    }
    
    void notifyFetched(ResultPage page) {
        nextPage++;

        /* Merge new links to result pages into our existing list of result pages to be visited.
         * Because we'll get many links multiple times, this method makes sure we only add pages
         * that are not yet on the list. */
        
        mergePages:
            for (WebLink newLink : page.pageLinks) {
                
                /* On the first result pages (the first 10 or so) we'll find a link to the
                 * initial page. That is never on our list because we retrieved it through the
                 * basic search URL at the start. Still we don't want to visit it again. */
                
                String urlstr = newLink.url.toExternalForm();
                if (urlstr.contains("start=0&") || urlstr.endsWith("&start=0"))
                    continue;
                
                for (WebLink oldLink : resultPages)
                    if (newLink.url.equals(oldLink.url))
                        continue mergePages;
                
                resultPages.add(newLink);
            }
    }
    
    /**
     * Merges a file match into the existing matches of this search result. If the file was
     * not previously found, the match is simply added to the list. If a match for the file
     * is already present, the new match is merged into the existing one (that is, any line
     * matches not previously known are added).
     * 
     * <p>If an existing file match object was found and the new match was merged into it,
     * the existing match is returned, otherwise the new match is returned. The return
     * value is therefore always the file match object that represents the file in this
     * search result instance in the future.
     * 
     * @param match a new line match to be added to the search result
     * @return the new or merged file match 
     */
    FileMatch mergeFileMatch(FileMatch match) {
        for (FileMatch existing : matches)
            if (existing.getFullName().equals(match.getFullName())) {
                lineCount += existing.merge(match);
                return existing;
            }
        
        matches.add(match);
        dirNames.add(match.getDirectory());
        lineCount += match.getLines().size();
        
        return match;
    }
    
    void mergeLines(FileMatch fileMatch, Collection<LineMatch> lines) {
        for (LineMatch line : lines) {
            if (fileMatch.addLine(line))
                lineCount++;
        }
        fileMatch.setUnabridged();
    }
    
    void setPageLimitTriggered(boolean limitTriggered) {
        this.limitTriggered = limitTriggered;
    }
    
    void setAborted(boolean aborted) {
        this.aborted = aborted;
    }
    
}
