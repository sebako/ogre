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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Perser for the pages returned by OpenGrok servers.
 *
 * @author Sebastian Koppehel
 */
public class ResultParser {
    
    private static class OgreParseException extends ScraperException {
        
        OgreParseException(String message) {
            super(message);
        }
        
        OgreParseException(String message, Throwable cause) {
            super(message, cause);
        }
        
        @Override
        public String getMessage() {
            return "Error parsing XHTML response: " + super.getMessage();
        }
        
    }
    
    private Document doc;
    private URL docURL;
    
    ResultParser(FetchResponse response) {
        doc = response.document;
        docURL = response.url;
    }
    
    ResultPage parsePage() throws OgreParseException {
        Element resultTable = elementForPath("/html/body/div[@id='page']/div[@id='results']/table", doc);
        
        // On "no results" pages, the table isn't there
        if (resultTable == null) {
            ResultPage result = new ResultPage();
            result.fileMatches = Collections.emptyList();
            result.pageLinks = Collections.emptyList();
            return result;
        }
        
        List<FileMatch> matches = new ArrayList<>();
        
        String dir = null;
        for (Element tr : elementsForPath("tr", resultTable)) {
            if ("dir".equals(tr.getAttribute("class")))
                dir = parseDirRow(tr);
            else {
                if (dir == null)
                    throw new OgreParseException("File row without preceding dir row in result list");
                matches.add(parseFileRow(dir, tr));
            }
        }
        
        List<WebLink> pageLinks = new ArrayList<>();
        for (Element link : elementsForPath("/html/body/div[@id='page']/div[@id='results']/p[@class='slider'][1]/a[@class='more']", doc)) {
            try {
                URL href = new URL(docURL, link.getAttribute("href"));
                pageLinks.add(new WebLink(href, docURL));
            }
            catch (MalformedURLException e) {
                // then just don't add it
            }
        }
        
        ResultPage result = new ResultPage();
        result.fileMatches = matches;
        result.pageLinks = pageLinks;
        
        return result;
    }
    
    private static String parseDirRow(Element rowElem) throws OgreParseException {
        String path = "td[1]/a[1]";
        return elementForPath(path, rowElem).getTextContent();
    }
    
    private FileMatch parseFileRow(String dir, Element rowElem) throws OgreParseException {
        Element fileLink = elementForPath("td[@class='f']/a", rowElem);
        if (fileLink == null)
            throw new OgreParseException("Table column contains no link to file");
        
        FileMatch hit;
        try {
            /* Hm... what happens if "href" attribute isn't set? The docURL becomes the xref link?
             * We might want to handle that case. */
            URL href = new URL(docURL, fileLink.getAttribute("href"));
            hit = new FileMatch(dir, fileLink.getTextContent(), new WebLink(href, docURL));
        }
        catch (MalformedURLException e) {
            hit = new FileMatch(dir, fileLink.getTextContent(), null);
        }
        
        for (Element lineLink : elementsForPath("td/tt[@class='con']/a", rowElem)) {
            // Actual line links are marked as class "s"
            if ("s".equals(lineLink.getAttribute("class")))
                hit.addLine(parseLine(lineLink));
            
            /* No-class link tags show up when all lines with hits are not shown in the
             * result table (for usability considerations presumably). They contain the
             * URL for the complete list of line hits. However, these would have to be
             * fetched separately, so we only mark the file hit as abridged at this point. */
            
            else if (lineLink.getAttribute("class").length() == 0) {
                try {
                    URL href = new URL(docURL, lineLink.getAttribute("href"));
                    hit.setAbridged(new WebLink(href, docURL));
                }
                catch (MalformedURLException e) {
                    hit.setAbridged(null);
                }
            }
        }
        
        return hit;
    }
    
    /**
     * Parses a document containing the full list of line matches for a file.
     * The file match object is amended to record all line matches.
     * 
     * @param fileMatch file match object that will be amended
     * @return 
     */
    List<LineMatch> parseMore() throws OgreParseException {
        List<LineMatch> lines = new ArrayList<>();
        
        String path = "/html/body/div[@id='page']/div[@id='content']/div[@id='more']/pre/a[@class='s']";
        for (Element el : elementsForPath(path, doc))
            lines.add(parseLine(el));
        
        /* Older versions of OpenGrok don't have the "content" div. */
        
        path = "/html/body/div[@id='page']/div[@id='more']/pre/a[@class='s']";
        for (Element el : elementsForPath(path, doc))
            lines.add(parseLine(el));
        
        return lines;
    }
    
    private LineMatch parseLine(Element lineLink) throws OgreParseException {
        NodeList nl = lineLink.getChildNodes();
        
        int lineNumber = -1;
        StringBuilder text = new StringBuilder();
        List<Integer> positions = new ArrayList<>();
        
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.TEXT_NODE)
                text.append(n.getTextContent());
            
            else if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) n;
                if ("span".equals(el.getTagName())) {
                    try {
                        lineNumber = Integer.parseInt(el.getTextContent());
                    }
                    catch (NumberFormatException e) {
                        throw new OgreParseException("Error parsing line number: "+ e.getMessage(), e);
                    }
                }
                else if ("b".equals(el.getTagName())) {
                    positions.add(text.length());
                    text.append(el.getTextContent());
                    positions.add(text.length());
                }
            }
        }
        
        if (lineNumber == -1)
            throw new OgreParseException("No line number found");
        
        try {
            URL href = new URL(docURL, lineLink.getAttribute("href"));
            return new LineMatch(lineNumber, text.toString(), new WebLink(href, docURL), positions);
        }
        catch (MalformedURLException e) {
            return new LineMatch(lineNumber, text.toString(), null, positions);
        }
        
    }
    
    private static Element elementForPath(String xpathExpression, Object source) throws OgreParseException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            Node node = (Node) xpath.evaluate(xpathExpression, source, XPathConstants.NODE);
            return (Element) node;
        }
        catch (XPathExpressionException e) {
            // Shouldn't happen
            throw new OgreParseException("Internal: incorrect XPath expression", e);
        }
    }
    
    private static List<Element> elementsForPath(String xpathExpression, Object source) throws OgreParseException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            NodeList nl = (NodeList) xpath.evaluate(xpathExpression, source, XPathConstants.NODESET);
            if (nl == null)
                throw new OgreParseException("Could not find elements for path: " + xpathExpression);
            
            List<Element> elements = new ArrayList<>(nl.getLength());
            for (int i = 0; i < nl.getLength(); i++)
                elements.add((Element) nl.item(i));
            return elements;
        }
        catch (XPathExpressionException e) {
            // Shouldn't happen
            throw new OgreParseException("Internal: incorrect XPath expression", e);
        }
    }
    
}
