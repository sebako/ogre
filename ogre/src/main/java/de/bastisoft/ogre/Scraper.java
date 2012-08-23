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
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.bastisoft.ogre.event.ProgressListener;
import de.bastisoft.ogre.event.ResultReceiver;
import de.bastisoft.ogre.event.ProgressListener.Phase;

public class Scraper {
    
    public static final String VERSION = "0.1";
    private static final String USER_AGENT = "OpenGrokScraper/" + VERSION;
    
    private URL basicURL;
    private Proxy proxy;
    private boolean fetchLines;
    private boolean fetchLinesLast;
    
    private Collection<ProgressListener> progressListeners;
    private Collection<ResultReceiver> resultReceivers;
    
    {
        fetchLines = true;
        fetchLinesLast = true;
        progressListeners = new ArrayList<>();
        resultReceivers = new ArrayList<>();
    }
    
    public Scraper(URL path, Proxy proxy) {
        this.basicURL = path;
        this.proxy = proxy;
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
    
    private void notifyNewFileMatches(Collection<FileMatch> matches) {
        for (ResultReceiver r : resultReceivers)
            r.newFileMatches(matches);
    }
    
    private void notifyNewLineMatches(FileMatch match) {
        for (ResultReceiver r : resultReceivers)
            r.newLineMatches(match);
    }
    
    public List<FileMatch> search(String query, String defs, String refs, String path, String project) throws ScraperException {
        StringBuilder params = new StringBuilder();
        
        append(params, query, "q");
        append(params, defs, "defs");
        append(params, refs, "refs");
        append(params, path, "path");
        append(params, project, "project");
        
        int current = 0;
        int overall = 1;
        
        try {
            List<FileMatch> results = new ArrayList<>();
            List<WebLink> pages = new ArrayList<>();
            
            notifyProgress(Phase.INITIAL, current, overall);
            URL searchURL = new URL(basicURL, "search?" + params);
            ResultPage page = new ResultParser(fetch(new WebLink(searchURL, null)), searchURL).parsePage();
            pages.addAll(page.pageLinks);
            
            int nextPage = 0;
            overall += pages.size();
            
            while (page != null) {
                Collection<FileMatch> newMatches = new ArrayList<>();
                
                addFileMatches:
                    for (FileMatch match : page.fileMatches) {
                        for (FileMatch existing : results)
                            if (existing.getFullName().equals(match.getFullName())) {
                                existing.merge(match);
                                notifyNewLineMatches(existing);
                                continue addFileMatches;
                            }
                        newMatches.add(match);
                    }
                
                results.addAll(newMatches);
                notifyNewFileMatches(newMatches);
                
                overall += addPageLinks(pages, page.pageLinks);
                
                if (fetchLines) {
                    for (FileMatch fileMatch : page.fileMatches)
                        if (fileMatch.abridged())
                            overall++;
                    
                    if (!fetchLinesLast) {
                        for (FileMatch fileMatch : page.fileMatches)
                            if (fileMatch.abridged()) {
                                notifyProgress(Phase.LINES, ++current, overall);
                                Document doc = fetch(fileMatch.getMoreLink());
                                new ResultParser(doc, fileMatch.getMoreLink().url).parseMore(fileMatch);
                                notifyNewLineMatches(fileMatch);
                            }
                    }
                }
                
                if (nextPage < pages.size()) {
                    notifyProgress(Phase.FILES, ++current, overall);
                    WebLink nextLink = pages.get(nextPage++);
                    page = new ResultParser(fetch(nextLink), nextLink.url).parsePage();
                }
                else
                    page = null;
            }
            
            if (fetchLines && fetchLinesLast)
                for (FileMatch fileMatch : results)
                    if (fileMatch.abridged()) {
                        notifyProgress(Phase.LINES, ++current, overall);
                        Document doc = fetch(fileMatch.getMoreLink());
                        new ResultParser(doc, fileMatch.getMoreLink().url).parseMore(fileMatch);
                        notifyNewLineMatches(fileMatch);
                    }
            
            return results;
        }
        catch (IOException | ParserConfigurationException | SAXException e) {
            throw new ScraperException("Error executing search query: " + e.getMessage(), e);
        }
    }
    
    private static int addPageLinks(List<WebLink> links, List<WebLink> newLinks) {
        int added = 0;
        
        outer:
            for (WebLink newLink : newLinks) {
                String urlstr = newLink.url.toExternalForm();
                if (urlstr.contains("start=0&") || urlstr.endsWith("&start=0"))
                    continue;
                
                for (WebLink oldLink : links)
                    if (newLink.url.equals(oldLink.url))
                        continue outer;
                
                links.add(newLink);
                added++;
            }
        
        return added;
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
    
    private Document fetch(WebLink link) throws IOException, ParserConfigurationException, SAXException {
        HttpURLConnection conn = (HttpURLConnection) (proxy != null ? link.url.openConnection(proxy) : link.url.openConnection());
        conn.setRequestProperty("User-Agent", USER_AGENT);
        if (link.referer != null)
            conn.setRequestProperty("Referer", link.referer.toExternalForm());
        try (InputStream in = conn.getInputStream()) {
            return new CorrectingReader(in, null).parse();
        }
    }
    
}
