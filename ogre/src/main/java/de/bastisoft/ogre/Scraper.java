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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.bastisoft.ogre.event.ProgressListener;
import de.bastisoft.ogre.event.ProgressListener.Phase;
import de.bastisoft.ogre.event.ResultReceiver;

public class Scraper {
    
    public static final String VERSION = "0.1";
    private static final String USER_AGENT = "ogre/" + VERSION;
    
    private URL basicURL;
    private Proxy proxy;
    private int pageLimit;
    private boolean aborted;
    private boolean fetchLines;
    private boolean fetchLinesLast;
    
    private Collection<ProgressListener> progressListeners;
    private Collection<ResultReceiver> resultReceivers;
    
    {
        pageLimit = 20;
        fetchLines = true;
        fetchLinesLast = true;
        progressListeners = new ArrayList<>();
        resultReceivers = new ArrayList<>();
    }
    
    /**
     * Create a new scraper instance that uses a given URL as the search entry point,
     * and does not go through a proxy server.
     * 
     * @param path URL of the OpenGrok search form
     */
    public Scraper(URL path) {
        this(path, Proxy.NO_PROXY);
    }
    
    /**
     * Create a new scraper instance that uses a given URL as the search entry point,
     * and goes through a proxy server to access the server.
     * 
     * @param path URL of the OpenGrok search form
     * @param proxy proxy configuration to be used when accessing the server
     */
    public Scraper(URL path, Proxy proxy) {
        this.basicURL = path;
        this.proxy = proxy == null ? Proxy.NO_PROXY : proxy;
    }
    
    /**
     * Sets the maximum number of result pages that will be fetched in a search run.
     * A value of 0 or less means that no connection will be made to the server, and
     * consequently no results will be received.
     * 
     * <p>A result page in this sense is a list of files with matches, including
     * selected lines with matches from every file. For every file, there may or may
     * not be a link on the result page leading to another page with more line
     * matches for that file. Unless the scraper is configured to never fetch
     * additional lines (see {@link #setFetchLines}), the scraper follows those links,
     * and this will lead to more page retrievals in the general sense.
     * 
     * <p>Those retrievals <em>do not</em> add to the
     * result page count that is limited by this setting. In other words, there may
     * be more HTTP requests placed than the page limit during a search run. However,
     * if additional line fetching is switched off, this limit actually does impose
     * an effective upper limit on HTTP requests.
     * 
     * @param limit maximum number of result pages to fetch
     */
    public void setPageLimit(int limit) {
        pageLimit = limit;
    }
    
    /**
     * Configures whether or not this scraper will place requests to the OpenGrok server
     * to retrieve the full listing of lines with matches for a file, if a link to such
     * a listing is found on a results page.
     * 
     * <p>If set to <code>false</code>, file matches may still contain a full or partial
     * set of lines matches. If a link was found but has not been followed, the file match
     * will be marked as abridged.
     * 
     * @param fetchLines <code>true</code> if additional line matches are to be fetched
     * @see FileMatch#abridged()
     */
    public void setFetchLines(boolean fetchLines) {
        this.fetchLines = fetchLines;
    }
    
    /**
     * Configures whether the scraper will fetch additional line matches only after all
     * result pages have been retrieved. This applies only in the case that additional
     * line are fetched at all (see {@link #setFetchLines}).
     * 
     * @param fetchLinesLast <code>true</code> if line matches are to be fetched at the
     *          end of the query
     */
    public void setFetchLinesLast(boolean fetchLinesLast) {
        this.fetchLinesLast = fetchLinesLast;
    }
    
    /**
     * Adds a progress listener that will receive progress updates, including partial
     * results, during a retrieval run.
     * 
     * @param listener progress listener
     */
    public void addProgressListener(ProgressListener listener) {
        progressListeners.add(listener);
    }
    
    public void abort() {
        aborted = true;
    }
    
    /**
     * Adds a result receiver that will receive the full result after a retrieval run
     * has been completed.
     * 
     * @param receiver result receiver
     */
    public void addResultReceiver(ResultReceiver receiver) {
        resultReceivers.add(receiver);
    }
    
    private void notifyProgress(Phase phase, int current, int overall) {
        for (ProgressListener l : progressListeners)
            l.progress(phase, current, overall);
    }
    
    private void notifyCounts(SearchResult result) {
        for (ProgressListener l : progressListeners)
            l.currentCounts(result.dirCount(), result.fileCount(), result.lineCount());
    }
    
    private void notifyNewFileMatches(Collection<FileMatch> matches) {
        for (ResultReceiver r : resultReceivers)
            r.newFileMatches(matches);
    }
    
    private void notifyNewLineMatches(FileMatch match) {
        for (ResultReceiver r : resultReceivers)
            r.newLineMatches(match);
    }
    
    /**
     * Places a search with the OpenGrok server, retrieves the results and returns them to
     * the caller. A call to this method can and typically will lead to multiple HTTP requests
     * to the server.
     * 
     * <p>Not all query fields have to be used. If a query field passed to this method is
     * <code>null</code>, or empty, or consists only of whitespace, the field will not be
     * included in the search request.
     * 
     * @param query search terms for the OpenGrok "Full search" field
     * @param defs search terms for the OpenGrok "Definition" field
     * @param refs search terms for the OpenGrok "Symbol" field
     * @param path search terms for the OpenGrok "File path" field
     * @param hist search terms for the OpenGrok "History" field
     * @param project the project(s) to search for multi-project OpenGrok servers
     * @return list of file matches returned by the search
     * @throws ScraperException if communication failures prevent a successful completion of
     *          the query, or if an error is encountered while parsing the result pages
     */
    public SearchResult search(String query, String defs, String refs, String path, String hist, String project) throws ScraperException {
        aborted = false;
        
        StringBuilder params = new StringBuilder();
        
        append(params, query, "q");
        append(params, defs, "defs");
        append(params, refs, "refs");
        append(params, path, "path");
        append(params, hist, "hist");
        append(params, project, "project");
        
        try {
            WebLink basicLink = followRedirect(new WebLink(basicURL, null));
            SearchResult result = new SearchResult(new WebLink(new URL(basicLink.url, "search?" + params), null));
            
            int current = 0;
            int pagecount = 0;
            
            WebLink next = null;
            while ((next = result.nextPage()) != null && pagecount < pageLimit && !aborted) {
                int pending = result.unfetchedPageCount();
                if (fetchLines) pending += result.abridgedFileCount();
                notifyProgress(Phase.FILES, current, pending);
                current++;
                pagecount++;
                
                ResultPage page = new ResultParser(fetch(next)).parsePage();
                result.notifyFetched(page);
                
                Collection<FileMatch> newMatches = new ArrayList<>();
                for (FileMatch match : page.fileMatches) {
                    FileMatch merged = result.mergeFileMatch(match);
                    if (merged != match)
                        notifyNewLineMatches(merged);
                    else
                        newMatches.add(match);
                }
                
                notifyNewFileMatches(newMatches);
                notifyCounts(result);
                
                if (fetchLines && !fetchLinesLast) {
                    pending = result.unfetchedPageCount() + result.abridgedFileCount();
                    FileMatch match;
                    while ((match = result.nextAbridgedFile()) != null && !aborted) {
                        notifyProgress(Phase.LINES, current++, pending--);
                        result.mergeLines(match, new ResultParser(fetch(match.getMoreLink())).parseMore());
                        notifyNewLineMatches(match);
                        notifyCounts(result);
                    }
                }
            }
            
            if (fetchLines && fetchLinesLast) {
                int pending = result.unfetchedPageCount() + result.abridgedFileCount();
                FileMatch match;
                while ((match = result.nextAbridgedFile()) != null && !aborted) {
                    notifyProgress(Phase.LINES, current++, pending--);
                    result.mergeLines(match, new ResultParser(fetch(match.getMoreLink())).parseMore());
                    notifyNewLineMatches(match);
                    notifyCounts(result);
                }
            }
            
            result.setPageLimitTriggered(next != null && pagecount >= pageLimit);
            result.setAborted(aborted);
            
            return result;
        }
        catch (IOException | ParserConfigurationException | SAXException e) {
            throw new ScraperException("Error executing search query: " + e.getMessage(), e);
        }
    }
    
    private static void append(StringBuilder sb, String value, String tag) {
        if (value == null)
            return;
        
        value = value.trim();
        if (value.length() == 0)
            return;
        
        if (sb.length() > 0)
            sb.append("&");
        
        sb.append(tag);
        sb.append("=");
        
        try {
            sb.append(URLEncoder.encode(value, "UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            // ...
        }
    }
    
    /**
     * Checks a URL to see if it is redirected by the server. The returned WebLink object retains
     * the original referer but, if the URL was redirected, contains a different destination URL.
     * If there is no redirection, the new WebLink has the same URL as the original one.
     * 
     * @param link URL to check for redirection
     * @return new link, possibly redirected
     * @throws IOException if there's an error during the request
     */
    private WebLink followRedirect(WebLink link) throws IOException {
        /* This is pretty horrible - we do a normal GET with a redirection-following HttpURLConnection,
         * look at the returned URL, and throw away the content. We should use a proper HTTP library
         * and issue a HEAD instead. */
        
        HttpURLConnection conn = (HttpURLConnection) (proxy != null ? link.url.openConnection(proxy) : link.url.openConnection());
        conn.setRequestProperty("User-Agent", USER_AGENT);
        if (link.referer != null)
            conn.setRequestProperty("Referer", link.referer.toExternalForm());
        try (InputStream in = conn.getInputStream()) {
            while (in.read() > -1);
            return new WebLink(conn.getURL(), link.referer);
        }
    }
    
    private FetchResponse fetch(WebLink link) throws IOException, ParserConfigurationException, SAXException {
        HttpURLConnection conn = (HttpURLConnection) (proxy != null ? link.url.openConnection(proxy) : link.url.openConnection());
        conn.setRequestProperty("User-Agent", USER_AGENT);
        if (link.referer != null)
            conn.setRequestProperty("Referer", link.referer.toExternalForm());
        try (InputStream in = conn.getInputStream()) {
            return new FetchResponse(new CorrectingReader(in, null).parse(), conn.getURL());
        }
    }
    
}
